package sherrloc.constraint.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import sherrloc.constraint.ast.Application;
import sherrloc.constraint.ast.Axiom;
import sherrloc.constraint.ast.Axiom.EdgeMatch;
import sherrloc.constraint.ast.Axiom.PremiseMatch;
import sherrloc.constraint.ast.ConstructorApplication;
import sherrloc.constraint.ast.Element;
import sherrloc.constraint.ast.FunctionApplication;
import sherrloc.constraint.ast.Inequality;
import sherrloc.constraint.ast.JoinElement;
import sherrloc.constraint.ast.MeetElement;
import sherrloc.constraint.ast.Relation;
import sherrloc.constraint.ast.Variable;
import sherrloc.constraint.ast.Constructor;
import sherrloc.graph.ConstraintGraph;
import sherrloc.graph.Edge;
import sherrloc.graph.EdgeCondition;
import sherrloc.graph.LeftEdge;
import sherrloc.graph.LeqCondition;
import sherrloc.graph.LeqEdge;
import sherrloc.graph.LeqRevCondition;
import sherrloc.graph.Node;
import sherrloc.graph.ReductionEdge;
import sherrloc.graph.RightEdge;
import sherrloc.graph.Variance;

/**
 * Implements the dynamic programming algorithm proposed by Chris Barrett, Riko
 * Jacob and Madhav Marathe. More details can be found in their paper
 * "Formal-language-constrained path problems". We also handle "meet" and "join"
 * when leq edges are inferred
 */
public class ShortestPathFinder extends CFLPathFinder {
	
	/** length of shortest paths */
//	private int[][] shortestLEQ;
//	private Map<EdgeCondition, Integer>[][] shortestLeft;
	private Map<Integer, Map<Integer, Integer>> shortestLEQ;
	private Map<Integer, Map<Integer, Map<EdgeCondition, Integer>>> shortestLeft;
	
	/** Lookup tables to find enumerable elements from components. These tables are used to infer extra edges for join/meet/constructors */
	private Map<Node, List<Node>>   joinElements = new HashMap<Node, List<Node>>();
	private Map<Node, List<Node>>   meetElements = new HashMap<Node, List<Node>>();
	private Map<Node, List<Node>>   consElements = new HashMap<Node, List<Node>>();
	
	/** for each node, we track a trace of solid edges (edges where both end nodes are black) to avoid recursion */
	private Map<Node, Set<Edge>>	trace = new HashMap<Node, Set<Edge>>();
	
	/** other fields */
	private int MAX = 100000;
	private PriorityQueue<ReductionEdge> queue;
	private boolean DEBUG = false;
	private boolean ACTIVE = true;
	
	/** optimizations */
	private final static boolean USE_SF = false;
	private boolean standardForm;
	private boolean actively_expanding;
	
	/**
	 * @param graph
	 *            A graph to be saturated
	 */
	public ShortestPathFinder(ConstraintGraph graph, boolean verbose, boolean isHypo) {
		super(graph);
		/** initialize data structures */
		standardForm = USE_SF && !isHypo;
		actively_expanding = ACTIVE; // && isHypo;
		queue = new PriorityQueue<ReductionEdge>(
				500, new Comparator<ReductionEdge>() {
					public int compare(ReductionEdge o1, ReductionEdge o2) {
						return o1.getLength() - o2.getLength();
					}
				});
		shortestLEQ = new HashMap<Integer, Map<Integer, Integer>>();
		shortestLeft = new HashMap<Integer, Map<Integer, Map<EdgeCondition, Integer>>>();
//		for (int i=0; i<size; i++) {
//			for (int j=0; j<size; j++) {
//				if (i == j)
//					shortestLEQ[i][j] = 0;
//				else
//					shortestLEQ[i][j] = MAX;
//			}
//		}
		initTables();
		long startTime = System.currentTimeMillis();
		initialize();
		saturation();
		long endTime = System.currentTimeMillis();
		if (verbose)
			System.out.println("path_finding time: " + (endTime - startTime));
	}
	
	@Override
	public ConstraintGraph getGraph() {
		return g;
	}
	
	/**
	 * initialize the lookup tables
	 */
	private void initTables() {
		for (Node n : g.getAllNodes()) {
			Element element = n.getElement();
			if (element instanceof JoinElement) {
				JoinElement je = (JoinElement) element;
				for (Element ele : je.getElements()) {
					Node toadd = g.getNode(ele);
					if (!joinElements.containsKey(toadd))
						joinElements.put(toadd, new ArrayList<Node>());
					joinElements.get(toadd).add(n);
				}
			} else if (element instanceof MeetElement) {
				MeetElement je = (MeetElement) element;
				for (Element ele : je.getElements()) {
					Node toadd = g.getNode(ele);
					if (!meetElements.containsKey(toadd))
						meetElements.put(toadd, new ArrayList<Node>());
					meetElements.get(toadd).add(n);
				}
			} else if (element instanceof ConstructorApplication || element instanceof FunctionApplication) {
				// notice that we only need to infer extra edges for concrete
				// constructors and functions, so there is no need to collect VariableApplication
				// Need to take care of nested applications (see test "consofvar4").
				initConsElements ((Application) element, n);
			}
		}
	}
	
	/**
	 * Add n as the super-element of all its sub-elements.
	 * 
	 * @param app An application element
	 * @param n The node for parameter app
	 */
	private void initConsElements (Application app, Node n) {
		for (Element ele : app.getElements()) {
			if (ele instanceof Application) {
				initConsElements((Application)ele, n);
			}
			else {
				Node toadd = g.getNode(ele);
				if (!consElements.containsKey(toadd))
					consElements.put(toadd, new ArrayList<Node>());
				if (!consElements.get(toadd).contains(n))
					consElements.get(toadd).add(n);
			}
		}
	}
	
	private int getShortestLeq (Node start, Node end) {
		if (shortestLEQ.containsKey(start.getIndex()) && shortestLEQ.get(start.getIndex()).containsKey(end.getIndex()))
			return shortestLEQ.get(start.getIndex()).get(end.getIndex());
		else
			return MAX;
//		return shortestLEQ[start.getIndex()][end.getIndex()];
	}
	
	private void setShortestLeq (Node start, Node end, int size) {
		if (!shortestLEQ.containsKey(start.getIndex()))
			shortestLEQ.put(start.getIndex(), new HashMap<Integer, Integer>());
		shortestLEQ.get(start.getIndex()).put(end.getIndex(), size);
	}
	
	// assume hasLeft(start, end)
	private int getShortestLeft (Node start, Node end, EdgeCondition inferredType) {
		return shortestLeft.get(start.getIndex()).get(end.getIndex()).get(inferredType);
	}
	
	private boolean hasShortestLeft (Node start, Node end, EdgeCondition inferredType) {
		if (shortestLeft.containsKey(start.getIndex()) && shortestLeft.get(start.getIndex()).containsKey(end.getIndex()) 
				&& shortestLeft.get(start.getIndex()).get(end.getIndex()).containsKey(inferredType))
			return true;
		else
			return false;
	}
	
	private void setShortestLeft (Node start, Node end, EdgeCondition inferredType, int size) {
		if (!shortestLeft.containsKey(start.getIndex()))
			shortestLeft.put(start.getIndex(), new HashMap<Integer, Map<EdgeCondition, Integer>>());
		if (!shortestLeft.get(start.getIndex()).containsKey(end.getIndex()))
			shortestLeft.get(start.getIndex()).put(end.getIndex(), new HashMap<EdgeCondition, Integer>());
		shortestLeft.get(start.getIndex()).get(end.getIndex()).put(inferredType, size);
	}
	
	@Override
	protected void inferEdge(Node start, Node end, EdgeCondition inferredType, int size, List<Evidence> evidence, boolean isAtomic) {	
		if (inferredType instanceof LeqCondition)
			System.out.println("Adding edge "+start.getElement()+"->"+end.getElement());
		addNextHop(start, end, inferredType, evidence);
		
		if (inferredType instanceof LeqCondition) {
			if (start.equals(end))
				return;
			queue.offer(new LeqEdge(start, end, size));
			setShortestLeq(start, end, size);
			if (isAtomic) {
				addAtomicLeqEdge(start.getIndex(), end.getIndex());
			}
		}
		else if (!inferredType.isReverse()) {
			queue.offer(new LeftEdge(start, end, size, inferredType));
			setShortestLeft(start, end, inferredType, size);
		}
		else {
			RightEdge newedge = new RightEdge(start, end, size, inferredType);
			int fIndex = start.getIndex(), tIndex = end.getIndex();
			if (!rightPath.containsKey(fIndex)) {
				rightPath.put(fIndex, new HashMap<Integer, List<RightEdge>>());
			}
			if (!rightPath.get(fIndex).containsKey(tIndex)) {
				rightPath.get(fIndex).put(tIndex, new ArrayList<RightEdge>());
			}
			rightPath.get(fIndex).get(tIndex).add(newedge);
		}
		
		if (DEBUG) {
			List<Edge> lst = new ArrayList<Edge>();
			getLeqPath(start, end, inferredType, lst, false);
			for (Edge e : lst)
				System.out.print(e.getFrom() + " --> " + e.getTo());
			System.out.println();
		}
	}	
	
	/**
	 * apply rule LEQ ::= LEQ LEQ
	 * 
	 * @param sIndex
	 *            Start node of the first LEQ edge
	 * @param fIndex
	 *            End node of the first LEQ edge (the same as the start node of
	 *            the second LEQ edge
	 * @param tIndex
	 *            End node of the second LEQ edge
	 */
	private void applyLeqLeq (Node from, Node mid, Node to) {
		if (from.equals(to))
			return;
		int disSF = getShortestLeq(from, mid), disFT = getShortestLeq(mid, to), disST = getShortestLeq(from, to);
		if (disSF + disFT < disST) {
			setShortestLeq(from, to, disSF + disFT);
			List<Evidence> evi = new ArrayList<Evidence>();
			evi.add(new Evidence(from, mid, LeqCondition.getInstance()));
			evi.add(new Evidence(mid, to, LeqCondition.getInstance()));
			inferEdge(from, to, LeqCondition.getInstance(), disSF+disFT, evi, false);
		}
	}
		
	/**
	 * apply rule LEQ ::= LEFT RIGHT
	 * 
	 * @param leftS
	 *            Start node of the LEFT edge
	 * @param leftE
	 *            End node of the LEFT edge (the same as the start node of the
	 *            RIGHT edge
	 * @param rightE
	 *            End node of the RIGHT edge
	 * @param ec
	 *            Edge condition ({@link EdgeCondition}) of the LEFT edge
	 */
	private void applyLeftRight (Node from, Node mid, Node to, EdgeCondition ec) {
		if (ec != null && hasLeftEdge(from, mid) &&
				hasRightEdges(mid, to) && getShortestLeft(from, mid, ec) + 1 < getShortestLeq(from, to)) {
			for (RightEdge e : getRightEdges(mid, to)) {
				if (e != null && ec.matches(((RightEdge) e).cons)) {
					setShortestLeq(from, to, getShortestLeft(from, mid, ec) + 1);
					List<Evidence> evi = new ArrayList<Evidence>();
					evi.add(new Evidence(from, mid, ec));
					evi.add(new Evidence(mid, to, ((RightEdge) e).cons));
					inferEdge(from, to, LeqCondition.getInstance(), getShortestLeft(from, mid, ec) + 1, evi, true);
				}
			}
		}
	}
	
	private void applyRightLeft (Node from, Node mid, Node to, EdgeCondition ec) {
		if (ec != null && hasLeftEdge(mid, to) &&
				hasRightEdges(from, mid) && getShortestLeft(mid, to, ec) + 1 < getShortestLeq(from, to)) {
			for (RightEdge e : getRightEdges(from, mid)) {
				if (e != null && ec.matches(((RightEdge) e).cons)) {
					setShortestLeq(from, to, getShortestLeft(mid, to, ec) + 1);
					List<Evidence> evi = new ArrayList<Evidence>();
					evi.add(new Evidence(from, mid, ((RightEdge) e).cons));
					evi.add(new Evidence(mid, to, ec));
					inferEdge(from, to, LeqCondition.getInstance(), getShortestLeft(mid, to, ec) + 1, evi, true);
				}
			}
		}
	}
		
	/**
	 * apply rule LEFT ::= LEFT LEQ
	 * 
	 * @param leftS
	 *            Start node of the LEFT edge
	 * @param leftE
	 *            End node of the LEFT edge
	 * @param newE
	 *            End node of the inferred LEFT edge
	 * @param ec
	 *            Edge condition ({@link EdgeCondition}) of the LEFT edge
	 * @param useReverse
	 *            Use the reverse of LEQ edge, since the negative LEQ edges are
	 *            not explicitly represented in graph to save space
	 */
	private void applyLeftLeq (Node from, Node mid , Node to , EdgeCondition ec, boolean useReverse) {
		Node leqS = mid, leqE = to;
		if (useReverse) {
			leqS = to;
			leqE = mid;
		}
		
		if (ec != null && hasShortestLeft(from, mid, ec) 
				&& getShortestLeq (leqS, leqE) < MAX ) {
			int newDis = getShortestLeft(from, mid, ec) + getShortestLeq(leqS, leqE);
			int oldDis = MAX;
			if (hasShortestLeft(from, to, ec)) {
				oldDis = getShortestLeft(from, to, ec);
			}
			if (newDis < oldDis) {
				setShortestLeft(from, to, ec, newDis);
				List<Evidence> evi = new ArrayList<Evidence>();
				evi.add(new Evidence(from, mid, ec));
				if (!useReverse)
					evi.add(new Evidence(mid, to, LeqCondition.getInstance()));
				else
					evi.add(new Evidence(mid, to, LeqRevCondition.getInstance()));
				inferEdge(from, to, ec, newDis, evi, false);
			}
		}
	}	
		
	/**
	 * Finding the (shortest) reduction path for error diagnosis is an instance
	 * of the context-free-language-reachability problem with the following
	 * grammar:
	 * <p>
	 * leq := left right | leq leq left := left leq
	 * <p>
	 * We follow the dynamic programming algorithm proposed by Chris Barrett,
	 * Riko Jacob and Madhav Marathe. More details can be found in their paper
	 * "Formal-language-constrained path problems". We also handle contravariant
	 * parameters, "meet" and "join" when id edges are inferred
	 */
	protected void saturation() {
		Set<Node> allNodes = g.getAllNodes();
		
		int current_length = 0;
//		int count = 1;
//		long startTime = System.currentTimeMillis();
		while (!queue.isEmpty()) {	
			ReductionEdge edge = queue.poll();
//			System.out.println(count++);
//			startTime = System.currentTimeMillis();
			
			if (edge instanceof LeqEdge)
				tryAddingExtraEdges ((LeqEdge)edge);
			
			assert (current_length <= edge.getLength()) : "Error: got a smaller edge "+ current_length + " " + edge.getLength();
			
			current_length = edge.getLength();
									
			Node from = edge.getFrom();
			Node to = edge.getTo();
				
			for (Node iNode : allNodes) {
//				if (iNode.equals(from) || iNode.equals(to))
//					continue;
				
				// first, use the reduction edge as the left part of a reduction rule
				if (edge instanceof LeqEdge) { 
					// LEQ = LEQ LEQ
					if ((!standardForm || (isDashedEdge(from) && isSolidEdge(to)))
							&& hasAtomicLeqEdge(to.getIndex(), iNode.getIndex()))
						applyLeqLeq(from, to, iNode);
				}
				else if (edge instanceof LeftEdge) {
					EdgeCondition ec = ((LeftEdge)edge).getCondition();
					
					// LEQ = LEFT RIGHT
					if (hasRightEdges(to, iNode))
						applyLeftRight(from, to, iNode, ec);

					// LEFT = LEFT LEQ (this reduction is redundant)
					if (standardForm && hasAtomicLeqEdge(to.getIndex(), iNode.getIndex()))
						applyLeftLeq(from, to, iNode, ec, ec.getVariance()==Variance.NEG);
				}
				
				// second, use the reduction edge as the right part of a reduction rule
				if (edge instanceof LeqEdge) {
					// LEQ = LEQ LEQ
					if (standardForm && isDashedEdge(iNode) && isSolidEdge(from)
							&& hasAtomicLeqEdge(from.getIndex(), to.getIndex()))
						applyLeqLeq(iNode, from, to);
					else if (!standardForm && hasAtomicLeqEdge(iNode.getIndex(), from.getIndex()))
						applyLeqLeq(iNode, from, to);
	
					// LEFT := LEFT LEQ
					if (hasLeftEdge(iNode, from)) {
						// FIXME: it seems that it makes no difference to make
						// sure either LEFT or LEQ is atomic. But it turns out a test program 
						// STUDENT08/20060408-23:13:58 takes longer to run.
//						&& (!StandardForm || hasAtomicLeqEdge(from.getIndex(), to.getIndex()))) {
						for (EdgeCondition ec : shortestLeft.get(iNode.getIndex()).get(from.getIndex()).keySet()) {
							if (getShortestLeft(iNode, from, ec)==1)
								applyLeftLeq(iNode, from, to, ec, ec.getVariance()==Variance.NEG);
						}
					}
				}
				
				if (edge instanceof LeftEdge) {
					EdgeCondition ec = ((LeftEdge)edge).getCondition();
					Element cons = ec.getCon();
					// LEQ = RIGHT LEFT
					if (hasRightEdges(iNode, from) && cons instanceof Constructor 
							&& ((Constructor)cons).getArity() == 1)
						applyRightLeft(iNode, from, to, ec);
				}
			}
		}
//		System.out.println("Saturation is done");
	}
	
	@Override
	public boolean hasLeqEdge(Node from, Node end) {
		return from.getElement().isBottom() || end.getElement().isTop() 
				|| from.getElement().equals(end.getElement()) 
				|| getShortestLeq(from, end) != MAX;
	}
	
	@Override
	public Set<Node> getFlowsFrom(Node to) {
		Set<Node> ret = new HashSet<Node>();
		for (Integer idx : shortestLEQ.keySet()) {
			if (shortestLEQ.get(idx).containsKey(to.getIndex()))
				ret.add(getGraph().getNode(idx));
		}
		return ret;
	}
	
	@Override
	public Set<Node> getFlowsTo(Node from) {
		Set<Node> ret = new HashSet<Node>();
		if (shortestLEQ.containsKey(from.getIndex())) {
			for (Integer idx : shortestLEQ.get(from.getIndex()).keySet()) {
				ret.add(getGraph().getNode(idx));
			}
		}
		return ret;
	}
	
	@Override
	public int leqEdgeLength(Node from, Node end) {
		if (from.getElement().isBottom() || end.getElement().isTop())
			return 1;
		else if (from.getElement().equals(end.getElement()))
			return 0;
		else
			return getShortestLeq(from, end);
	}
	
	@Override
	public boolean hasLeftEdge(Node from, Node to) {
		if (shortestLeft.containsKey(from.getIndex()) && shortestLeft.get(from.getIndex()).containsKey(to.getIndex())
				&& !shortestLeft.get(from.getIndex()).get(to.getIndex()).isEmpty())
			return true;
		else
			return false;
	}
	
	/**
	 * Return a path in the constraint graph so that a LEFT edge on
	 * <code>start, end</code> can be derived from constraints along the path.
	 * Return null when no such path exits
	 */
	public List<List<Edge>> getLeftPaths(Node start, Node end) {
		List<List<Edge>> paths = new ArrayList<List<Edge>>();
		if (!hasLeftEdge(start, end))
			return new ArrayList<List<Edge>>();
		else {
			for (EdgeCondition con : shortestLeft.get(start.getIndex()).get(end.getIndex()).keySet()) {
				List<Edge> lst = new ArrayList<Edge>();
				getLeqPath(start, end, con, lst, false);
				paths.add(lst);
			}
		}
		return paths;
	}
	
	/**
	 * Try to apply axioms that might utilized the newly added LeqEdge edge to
	 * infer new edges in the graph
	 */
	private void applyAxioms (LeqEdge edge) {
		for (Axiom rule : g.getRules()) {
			if (!rule.mayMatch(edge))
				continue;
			List<PremiseMatch> pmatches = rule.findMatchesInPremise(this);
			
			for (PremiseMatch pmatch : pmatches) {
			// apply all substitutions along the unification to conclusion
			for (Inequality ieq : rule.getConclusion()) {
				Element e1 = ieq.getFirstElement().subst(pmatch.map);
				Element e2 = ieq.getSecondElement().subst(pmatch.map);
				
				// expand new graph nodes when necessary
				boolean needExpansion = actively_expanding && !e1.hasQVars() && !e2.hasQVars() && pmatch.noGrayNodes && (g.hasElement(e1) || g.hasElement(e2));
				if (needExpansion) {
					if (!g.hasElement(e1)) {
						g.getNode(e1, true);
					}
					if (!g.hasElement(e2)) {
						g.getNode(e2, true);
					}
				}
				List<EdgeMatch> emlst = rule.findMatches (ieq.getFirstElement(), ieq.getSecondElement(), this, pmatch.map);
				for (EdgeMatch em : emlst) {
					if (pmatch.size < getShortestLeq(em.n1, em.n2))
						inferEdge(em.n1, em.n2, LeqCondition.getInstance(), pmatch.size, pmatch.evidences, true);
					if (ieq.getRelation() == Relation.EQ && pmatch.size < getShortestLeq(em.n2, em.n1))
						inferEdge(em.n2, em.n1, LeqCondition.getInstance(), pmatch.size, pmatch.evidences, true);
				}
			}
			}
		}
	}
	
	/**
	 * Actively create a new gray node (CONS subst.elem) if (CONS n.elem) is
	 * already in graph, as well as n and subst are not gray nodes. The
	 * direction between (CONS subst.elem) and (CONS n.elem) is determined by
	 * ltor and the variance of CONS: (CONS n.elem) --> (CONS subst.elem) iff
	 * ltor&&COVAR or !ltor&&CONTRA
	 * 
	 * @param n
	 * @param subst
	 */
	private void expandOneNode (Node n, Node subst, LeqEdge edge, boolean ltor) {
		if (consElements.containsKey(n) && !n.isGray() && !subst.isGray()) {
			List<Node> copy = new ArrayList<Node>(consElements.get(n)); 
			for (Node cplxNode : copy) {
				if (!cplxNode.isGray() || !trace.get(cplxNode).contains(edge)) {
					Application app = (Application) cplxNode.getElement();
					List<Application> nelems = app.replace(n.getElement(), subst.getElement());
//					boolean direction = (ltor&&(app.getVariance()==Variance.POS)) || (!ltor&&(app.getVariance()==Variance.NEG));
					for (Application nelem : nelems) {
						if (g.hasElement(nelem))
							continue;
						Node newnode =  g.getNode(nelem, true);
						trace.put(newnode, new HashSet<Edge>());
						if (cplxNode.isGray())
							trace.get(newnode).addAll(trace.get(cplxNode));
						trace.get(newnode).add(edge);
						initConsElements(nelem, newnode);
//						int size = getShortestLeq(n, subst);
//						List<Evidence> evi = new ArrayList<Evidence>();
//						Evidence e = new Evidence(n, subst, LeqCondition.getInstance());
//						evi.add(e);
//						System.out.println("trying "+cplxNode+" to "+newnode+" based on "+n+" to "+subst);
//						
//						if (direction && !hasLeftEdge(cplxNode, newnode))
//							inferEdge(cplxNode, newnode, LeqCondition.getInstance(), size, evi, true);
//						else
//							inferEdge(newnode, cplxNode, LeqCondition.getInstance(), size, evi, true);
					}
				}
			}
		}
	}
	
	/**
	 * Given a newly discovered LeqEdge, this function tries to identify extra
	 * LeqEdges by using the properties of meet, join and constructor
	 */
	private void tryAddingExtraEdges (LeqEdge edge) {
		Node from = edge.getFrom();
		Node to = edge.getTo();
		applyAxioms(edge);
		
		// if node "to" is an element of a meet label, add an leq edge from node
		// "from" to the meet element if it flows into all components
		if (meetElements.containsKey(to)) {
			for (Node meetnode : meetElements.get(to)) {
				MeetElement me = (MeetElement) meetnode.getElement();
				Node candidate = from;
				int candIndex = candidate.getIndex();
				int meetIndex = meetnode.getIndex();
				boolean success = true;
				
				if (hasLeqEdge(candidate, meetnode) || candIndex == meetIndex)
					continue;
				for (Element e : me.getElements()) {
					if (!hasLeqEdge(candidate, g.getNode(e))) {
						success = false;
						break;
					}
				}
				if (success) {
					List<Evidence> evidences = new ArrayList<Evidence>();
					int size = 0;
					for (Element e : me.getElements()) {
						if (getShortestLeq(candidate, g.getNode(e)) < MAX) {
							size += getShortestLeq(candidate, g.getNode(e));
							evidences.add(new Evidence(candidate, g.getNode(e), LeqCondition.getInstance()));
						}
						else {
							size ++;
						}
					}
					if (!hasLeqEdge(candidate, meetnode))
						inferEdge(candidate, meetnode, LeqCondition.getInstance(), size, evidences, true);
				}
			}
		}
		
		// if node "from" is an element of a join label, add an leq edge from 
		// the join element to node "to" if all components flow into it
		if (joinElements.containsKey(from)) {
			for (Node joinnode : joinElements.get(from)) {
				JoinElement je = (JoinElement) joinnode.getElement();
				Node candidate = to;
				int candIndex = candidate.getIndex();
				int joinIndex = joinnode.getIndex();
				boolean success = true;

				if (hasLeqEdge(joinnode, candidate) || joinIndex == candIndex)
					continue;
				for (Element e : je.getElements()) {
					if (!hasLeqEdge(g.getNode(e), candidate)) {
						success = false;
						break;
					}
				}
				if (success) {
					List<Evidence> evidences = new ArrayList<Evidence>();
					int size = 0;
					for (Element e : je.getElements()) {
						if (getShortestLeq(g.getNode(e), candidate) < MAX) {
							size += getShortestLeq(g.getNode(e), candidate);
							evidences.add(new Evidence(g.getNode(e), candidate, LeqCondition.getInstance()));
						}
						else {
							size++;
						}
					}
					if (!hasLeqEdge(joinnode, candidate))
						inferEdge(joinnode, candidate, LeqCondition.getInstance(), size, evidences, true);
				}
			}
		}
		
		// if node "from" and "to" belong to same constructor, check if this new
		// link enables a leq relation on the constructor application
		if (actively_expanding && (consElements.containsKey(from) || consElements.containsKey(to))) {
			expandOneNode(from, to, edge, true);
			expandOneNode(to, from, edge, false);

			if (consElements.containsKey(from) && consElements.containsKey(to)) {
			for (Node cnFrom : consElements.get(from)) {
				for (Node cnTo : consElements.get(to)) {
					// make sure this is "ce1", not the swapped one when the constructor is contravariant
					// the elements can either be ConstructorApplication, or FunctionApplication
					int arity;
					if (cnFrom.getElement() instanceof ConstructorApplication && cnTo.getElement() instanceof ConstructorApplication) {
						ConstructorApplication ce1 = (ConstructorApplication) cnFrom.getElement();
						arity = ce1.getCons().getArity();
						if (!ce1.getCons().equals(((ConstructorApplication)cnTo.getElement()).getCons()))
							continue;
					}
					else if (cnFrom.getElement() instanceof FunctionApplication && cnTo.getElement() instanceof FunctionApplication) {
						FunctionApplication fe1 = (FunctionApplication) cnFrom.getElement();
						arity = fe1.getFunc().getArity();
						if (!fe1.getFunc().equals(((FunctionApplication)cnTo.getElement()).getFunc()))
							continue;
					}
					else
						continue;
					
					Application ce1 = (Application) cnFrom.getElement();
					Application ce2 = (Application) cnTo.getElement();
					// depending on the variance of the parameters, we need to
					// infer an edge from cnFrom to cnTo (covariant), cnTo to cnFrom
					// (contravariant), or both (invariant)
					boolean ltor = false, rtol = false;
					if (ce1.getVariance().equals(Variance.POS))
						ltor = true;
					else if (ce1.getVariance().equals(Variance.NEG))
						rtol = true;
					else if (ce1.getVariance().equals(Variance.NONE)) {
						ltor = true;
						rtol = true;
					} 

					// only infer new edges when none already exists
					if ( (ltor && !hasLeqEdge(cnFrom, cnTo)) || (rtol && !hasLeqEdge(cnTo, cnFrom)) ) {
						// check if all elements flows into another constructor
						boolean success = true;

						for (int i = 0; i < arity; i++) {
							Element e1 = ce1.getElements().get(i);
							Element e2 = ce2.getElements().get(i);
							/* it seems we shouldn't test if e1 and e2 are Variables. But it breaks 2 ocaml test cases. Need to justify this change */
							if (!hasLeqEdge(g.getNode(e1), g.getNode(e2)) /*|| e1 instanceof Variable || e2 instanceof Variable*/) {
								success = false;
								break;
							}
							// test the other direction for invariant parameters
							if (ce1.getVariance().equals(Variance.NONE)) {
								if (!hasLeqEdge(g.getNode(e2), g.getNode(e1))) {
									success = false;
									break;
								}
							}
						}
												
						if (success) {
							List<Evidence> evidences = new ArrayList<Evidence>();
							int size=0;
							for (int i = 0; i < arity; i++) {
								Element e1 = ce1.getElements().get(i);
								Element e2 = ce2.getElements().get(i);
								if (getShortestLeq(g.getNode(e1), g.getNode(e2)) < MAX) {
									size += getShortestLeq(g.getNode(e1), g.getNode(e2));
									evidences.add(new Evidence(g.getNode(e1), g.getNode(e2), LeqCondition.getInstance()));
								}
								else {
									size ++;
								}
							}
							if (ltor && !hasLeqEdge(cnFrom, cnTo))
								inferEdge(cnFrom, cnTo, LeqCondition.getInstance(), size, evidences, true);
							if (rtol && !hasLeqEdge(cnTo, cnFrom))
								inferEdge(cnTo, cnFrom, LeqCondition.getInstance(), size, evidences, true);
						}
					}
				}
			}}
		}
	}
}
