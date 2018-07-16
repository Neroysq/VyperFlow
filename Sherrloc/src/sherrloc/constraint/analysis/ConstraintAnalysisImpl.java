package sherrloc.constraint.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sherrloc.constraint.ast.Application;
import sherrloc.constraint.ast.Axiom;
import sherrloc.constraint.ast.Axiom.PremiseMatch;
import sherrloc.constraint.ast.Constraint;
import sherrloc.constraint.ast.Constructor;
import sherrloc.constraint.ast.Element;
import sherrloc.constraint.ast.Function;
import sherrloc.constraint.ast.Inequality;
import sherrloc.constraint.ast.JoinElement;
import sherrloc.constraint.ast.MeetElement;
import sherrloc.constraint.ast.Relation;
import sherrloc.constraint.ast.Variable;
import sherrloc.diagnostic.UnsatPaths;
import sherrloc.graph.ConstraintEdge;
import sherrloc.graph.ConstraintGraph;
import sherrloc.graph.ConstraintPath;
import sherrloc.graph.DummyEdge;
import sherrloc.graph.Edge;
import sherrloc.graph.Node;
import sherrloc.graph.Variance;

/**
 * This class identifies satisfiable and unsatisfiable constraints in a
 * constraint graph.
 */
public class ConstraintAnalysisImpl implements ConstraintAnalysis {
	private boolean isVerbose;
	private boolean isRec;
	private boolean isGenHypo;
	private boolean DEBUG = false;
	private boolean PASSIVE = false;
	private int expansion_time = 0;
	
	private Map<Element, Set<Element>> testedL = new HashMap<Element, Set<Element>>();
	private Map<Element, Set<Element>> testedR = new HashMap<Element, Set<Element>>();
	private Map<Element, Set<Element>> expandedL = new HashMap<Element, Set<Element>>();
	private Map<Element, Set<Element>> expandedR = new HashMap<Element, Set<Element>>();

	/**
	 * @param isHypo
	 *            True if SHErrLoc is inferring missing hypothesis
	 * @param isVerbose
	 *            True to collect data for evaluation
	 * @param isRec
	 *            True if recursion is allowed
	 */
	public ConstraintAnalysisImpl(boolean isGenHypo, boolean isVerbose, boolean isRec) {
		this.isGenHypo = isGenHypo;
		this.isVerbose = isVerbose;
		this.isRec = isRec;
	}

	/**
	 * Return an instance of constraint analysis. Currently, the only analysis
	 * implemented is {@link ShortestPathFinder}
	 * 
	 * @return An constraint analysis algorithm
	 */
	private PathFinder getPathFinder(ConstraintGraph graph) {
		return new ShortestPathFinder(graph, isVerbose, false);
	}

	@Override
	public UnsatPaths genErrorPaths(ConstraintGraph graph) {
		UnsatPaths unsatPaths = new UnsatPaths();
		
		if (isVerbose)
			System.out.println("graph_size: " + graph.getAllNodes().size());

		// saturate constraint graph
		PathFinder finder = getPathFinder(graph);

		if (!isRec) {
		for (Node node : graph.getAllNodes()) {
			// when recursion is not allowed, constraints such as "x = list x" is unsatisfiable
			if (finder.hasLeftEdge(node, node)) {
				List<List<Edge>> paths = finder.getLeftPaths(node, node);
				for (List<Edge> l : paths) {
					ConstraintPath path = new ConstraintPath(l, finder, graph.getEnv());
					unsatPaths.addUnsatPath(path);
					if (DEBUG) {
						System.out.println("****** Infinite path ******");
						System.out.println(path);
					}
				}
				continue;
			}
			// go one step ahead
			else {
				for (Node m : graph.getAllNodes()) {
					if (finder.hasLeftEdge(node, m) && finder.hasLeftEdge(m, node)) {
						List<List<Edge>> paths = finder.getLeftPaths(node, m);
						for (List<Edge> l1 : paths) {
							for (List<Edge> l2 : finder.getLeftPaths(m, node)) {
								List<Edge> lst = new ArrayList<Edge>();
								lst.addAll(l1);
								lst.addAll(l2);
								ConstraintPath path = new ConstraintPath(lst, finder, graph.getEnv());
								unsatPaths.addUnsatPath(path);
								if (DEBUG) {
									System.out.println("****** Infinite path ******");
									System.out.println(path);
								}
							}
						}
					}
				}
			}
			// TODO: need to generalize the algorithm to more general cases
		}
		}
		
		for (Axiom rule : graph.getRules()) {
			List<PremiseMatch> pmatches = rule.findMatchesInPremise(finder);
			
			for (PremiseMatch pmatch : pmatches) {
			// apply all substitutions along the unification to conclusion
			for (Inequality ieq : rule.getConclusion()) {
				Element m1 = ieq.getFirstElement().subst(pmatch.map);
				Element m2 = ieq.getSecondElement().subst(pmatch.map);

				if (m1 instanceof Application && (m2 instanceof Constructor || m2 instanceof Function)) {
					List<Node> matched = graph.getMatchedNodes(m2);
					for (Node n : matched) {
						if (!testedL.containsKey(n.getElement()))
							testedL.put(n.getElement(), new HashSet<Element>());
						testedL.get(n.getElement()).add(m1);
					}
				}
				else if (m2 instanceof Application && (m1 instanceof Constructor || m1 instanceof Function)) {
					List<Node> matched = graph.getMatchedNodes(m1);
					for (Node n : matched) {
						if (!testedR.containsKey(n.getElement()))
							testedR.put(n.getElement(), new HashSet<Element>());
						testedR.get(n.getElement()).add(m2);
					}
				}				
			}
			}
		}
		
		Set<Node> allNodes = new HashSet<Node>(graph.getAllNodes());
		for (Node start : allNodes) {
			for (Node end : allNodes) {
				// avoid returning duplicated edges when only equalities are used
				if (start.getIndex() <= end.getIndex())
					continue;

				// the test on the other direction can be avoided if
				// 1) not inferring missing hypothesis
				// 2) all constraints along the path are equalities
				boolean needtest = true;
				
				// test if a partial ordering can be inferred
				if (!(start.getElement() instanceof JoinElement) && !(end.getElement() instanceof MeetElement) && finder.hasLeqEdge(start, end)) {
					List<Edge> l = finder.getPath(start, end);
					System.out.println("Comparing "+ start.getElement()+"-->"+end.getElement());
					if (skolemCheck(start.getElement(), end.getElement()))
						testConsistency(start.getElement(), end.getElement(), l, graph, finder, unsatPaths, false);
					else {
						ConstraintPath path = new ConstraintPath(l, finder, graph.getEnv());
						unsatPaths.addUnsatPath(path);
						if (DEBUG) {
							System.out.println("****** Skolem check fails ******");
							System.out.println(path);
						}
					}
				
					if (!isGenHypo) {
						boolean allEQ = false;
						for (Edge edge : l) {
							if (edge instanceof ConstraintEdge) {
								Constraint cons = ((ConstraintEdge)edge).getConstraint();
								if (cons.getRelation() == Relation.EQ) {
									allEQ = true;
								}
								else {
									allEQ = false;
									break;
								}
							}
						}
						needtest = !allEQ;
					}
				}
				
				if (needtest && !(end.getElement() instanceof JoinElement) && !(start.getElement() instanceof MeetElement) && finder.hasLeqEdge(end, start)) {
					List<Edge> l = finder.getPath(end, start);
					if (skolemCheck(start.getElement(), end.getElement()))
						testConsistency(end.getElement(), start.getElement(), l, graph, finder, unsatPaths, false);
					else {
						ConstraintPath path = new ConstraintPath(l, finder, graph.getEnv());
						unsatPaths.addUnsatPath(path);
						if (DEBUG) {
							System.out.println("****** Skolem check fails ******");
							System.out.println(path);
						}
					}
				}
			}
		}
		
		if (isVerbose)
			System.out.println("expansion_time: " + expansion_time);

		return unsatPaths;
	}
	
	/**
	 * Return true if the skolem check succeeds
	 */
	boolean skolemCheck (Element e1, Element e2) {
		if (e1 instanceof Variable) {
			return ((Variable)e1).getVarLevel() >= e2.getSkolemLevel();
		}
		if (e2 instanceof Variable) {
			return ((Variable)e2).getVarLevel() >= e1.getSkolemLevel();
		}
		return true;
	}
	
	void testConsistency (Element e1, Element e2, List<Edge> l, ConstraintGraph graph, PathFinder finder, UnsatPaths unsatPaths, boolean rec) {
		// ignore trivial cases
		if (e1.trivialEnd() || e2.trivialEnd()) {
			return;
		}

		// less interesting paths
		if (e1.isBottom() || e2.isTop())
			return;

		ConstraintPath path = new ConstraintPath(l, finder, graph.getEnv());
		if (path.isInformative()) {
			if (path.isUnsatPath()) {
				if (isVerbose)
					System.out.println("Cannot unify "+path.getFirstElement()+" with "+path.getLastElement());
				if (DEBUG) {
					System.out.println("****** Unsatisfiable path ******");
					System.out.println(path);
				}
				unsatPaths.addUnsatPath(path);
				path.setCause();
			}
			else {
				if (path.isValidPath()) {
					if (!rec)
						path.incSuccCounter();
				}
				else if (PASSIVE && path.isSatPath()) {
					long startTime = System.currentTimeMillis();
					expandGraph(e1, e2, l, graph, finder, unsatPaths);
					
					long endTime = System.currentTimeMillis();
					if (!rec)
						expansion_time += (endTime - startTime);
				}
			}
		}
	}
	
	private class VisitNode {
		Element start;
		Element end;
		List<Edge> path;
		
		public VisitNode(Element start, Element end, List<Edge> path) {
			this.start = start;
			this.end = end;
			this.path = path;
		}
	}
	
	/**
	 * Replace the variable in e with elements that flows into, or flows out of
	 * the variable, judged by parameter ltor
	 * 
	 * @param e Element to be replaced
	 * @param ltor True if the replacement uses elements that flows into variable in e
	 */
	private List<VisitNode> expandElement(Application e, Element other, List<Edge> sofar, boolean ltor, PathFinder finder) {
		List<VisitNode> ret = new ArrayList<VisitNode>();
		ConstraintGraph graph = finder.getGraph();
		
		// Tested tracks relations that have already tested
		// Gray tracks new elements to be explored (on fringe), so that a nested search wouldn't duplicate tests
		Map<Element, Set<Element>> tested, expanded;
		if (ltor) {
			tested = testedL;
			expanded = expandedL;
		}
		else {
			tested = testedR;
			expanded = expandedR;
		}
		if (!tested.containsKey(other))
			tested.put(other, new HashSet<Element>());
		if (!expanded.containsKey(other))
			expanded.put(other, new HashSet<Element>());
		if (tested.get(other).contains(e.getBaseElement()))
			return ret;
		tested.get(other).add(e.getBaseElement());
		
		boolean isContra = (e.getVariance() == Variance.NEG);
		for (Variable var : e.getVars()) {
			Node varnode = graph.getNode(var);
			Set<Node> replacements;
			if (isContra^ltor) 
				replacements = finder.getFlowsFrom(varnode);
			else
				replacements = finder.getFlowsTo(varnode);
			for (Node n : replacements) {
				List<Application> newelems = e.replace(var, n.getElement());
				for (Application newelem : newelems) {
					if (n.getElement() instanceof Variable) {
						tested.get(other).add(newelem.getBaseElement());
					}
					else if (!expanded.get(other).contains(newelem.getBaseElement()) 
							&& !graph.hasElement(newelem)) {
						expanded.get(other).add(newelem.getBaseElement());
						List<Edge> edgessofar = new ArrayList<Edge>();
						if (ltor) {
							edgessofar.add(new DummyEdge(graph.getNode(newelem), n, true));
							edgessofar.addAll(finder.getPath(n, varnode));
							edgessofar.add(new DummyEdge(varnode, graph.getNode(e), false));						
							edgessofar.addAll(sofar);
							ret.add(new VisitNode(newelem, other, edgessofar));
						}
						else {
							edgessofar.addAll(sofar);
							edgessofar.add(new DummyEdge(graph.getNode(e), n, true));
							edgessofar.addAll(finder.getPath(n, varnode));
							edgessofar.add(new DummyEdge(varnode, graph.getNode(newelem), false));						
							ret.add(new VisitNode(other, newelem, edgessofar));
						}
					}
				}
			}
		}
		return ret;
	}
	
	void expandGraph (Element e1, Element e2,  List<Edge> l, ConstraintGraph graph, PathFinder finder, UnsatPaths unsatPaths) {
		List<VisitNode> toVisit = null;
		
		if (e1.hasVars() && e1 instanceof Application && (e2 instanceof Constructor || e2 instanceof Function))
			toVisit = expandElement((Application)e1, e2, l, true, finder);
		else if (e2.hasVars() && e2 instanceof Application && (e1 instanceof Constructor || e1 instanceof Function))
			toVisit = expandElement((Application)e2, e1, l, false, finder);
		else
			return;
		
		for (VisitNode vnode : toVisit) {
			testConsistency(vnode.start, vnode.end, vnode.path, graph, finder, unsatPaths, true);
		}
	}
}
