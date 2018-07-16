package sherrloc.graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sherrloc.constraint.ast.Application;
import sherrloc.constraint.ast.Axiom;
import sherrloc.constraint.ast.Constraint;
import sherrloc.constraint.ast.ConstructorApplication;
import sherrloc.constraint.ast.Element;
import sherrloc.constraint.ast.EnumerableElement;
import sherrloc.constraint.ast.Hypothesis;
import sherrloc.constraint.ast.Inequality;
import sherrloc.constraint.ast.JoinElement;
import sherrloc.constraint.ast.MeetElement;
import sherrloc.constraint.ast.Position;
import sherrloc.constraint.ast.Relation;
import sherrloc.constraint.ast.Variable;
import sherrloc.constraint.ast.VariableApplication;
import sherrloc.util.StringUtil;

/**
 * A constraint graph is built on a set of constructors (including user-defined
 * ones), assumptions, as well as constraints as inputs.
 * <p>
 * This class builds a basic graph from the constraints, without saturation.
 * Different <code>PathFinders</code> are used to saturate the graph, which is
 * later used as inputs to infer the most likely error cause.
 */
public class ConstraintGraph extends Graph {
	private Hypothesis env;
	private List<Axiom> rules;
    
    private Set<String> files;                                          // source codes involved, only used for DOT files
    private final boolean PRINT_SRC = false;                     		// print corresponding source code in DOT files
    private Map<Element, Node> eleToNode = new HashMap<Element, Node>(); // map from AST elements to graph nodes
    private Map<Integer, Node> idxToNode = new HashMap<Integer, Node>(); // map from integers to graph nodes
    private int varCounter = 0;
    private boolean isSymmetric=true;
    
	/** Optimizations */
	private final boolean USE_OPT = false;
	private boolean OPT_AXIOMS = true;
	// A map from base elements (elements with no position info) to potentially multiple uses of the element
	// Useful for matching axioms in a graph.
	private Map<Element, List<Node>> baseToNodes = new HashMap<Element, List<Node>>();
	
	/**
	 * @param env
	 *            Global assumptions
	 * @param constraints
	 *            Constraints
	 */
    public ConstraintGraph (Hypothesis env, Set<Constraint> constraints, List<Axiom> axioms) {
        this(env, axioms);
    	/**
		 * generate the simple links from the constraints. handle constructors,
		 * meet and join later
		 */
		for (Constraint cons : constraints) {
			addOneConstraint(cons);
		}
    }
    
    /**
     * See {@link #ConstraintGraph(Hypothesis, Set)}
     */
    public ConstraintGraph (Hypothesis env, List<Axiom> axioms) {
        this.env = env;
    	this.files = new HashSet<String>();
        this.rules = axioms;
    }
                
	/**
	 * Lookup a node representing element <code>e</code> in graph. Create a
	 * fresh node if no such node exists
	 * 
	 * @param e
	 *            Element to find
	 * @return A node representing <code>e</code>
	 */
    public Node getNode (Element e) {
    	return getNode(e, false);
    }
        
    /**
	 * Lookup a node representing element <code>e</code> in graph. Create a
	 * fresh node if no such node exists
	 * 
	 * @param e
	 *            Element to find
	 * @param isGray
	 *            True if the node is created during graph saturation
	 * @return A node representing <code>e</code>
	 */
    public Node getNode (Element e, boolean isGray) {
    	if (! eleToNode.containsKey(e)) {
            Node n = new Node (varCounter, e, isGray);
            addNode(n);
            varCounter++;
            eleToNode.put(e, n);
            idxToNode.put(n.getIndex(), n);
            Element baseEle = e.getBaseElement();
            if (!baseToNodes.containsKey(baseEle))
            	baseToNodes.put(baseEle, new ArrayList<Node>());
            baseToNodes.get(baseEle).add(n);
        }
        return eleToNode.get(e);
    }
    
    /**
	 * Return a node with index idx. Return null if no such node exists
	 * 
	 * @param idx
	 *            Index of a node to be found
	 * @return The node with index idx. Null if no such node exists
	 */
    public Node getNode (int idx) {
    	return idxToNode.get(idx);
    }

	/**
	 * @param e
	 *            A constraint element
	 * @return True if the graph has a node representing <code>e</code>
	 */
    public boolean hasElement (Element e) {
    	return eleToNode.containsKey(e);
    }
    
    /**
     * @return A list of elements that represent baseElement. Return null if no such element exists.
     */
    public List<Node> getMatchedNodes(Element baseElement) {
		return baseToNodes.get(baseElement);
	}
    
	/**
	 * Adding a constraint to graph (add edges between nodes representing
	 * constraint elements)
	 * 
	 * @param cons
	 *            Constraint
	 */
    public void addOneConstraint (Constraint cons) {
		Node source = getNode(cons.getFirstElement());
		Node to = getNode(cons.getSecondElement());

		addLeqEdge(new ConstraintEdge(cons, source, to));

		if (cons.getRelation() == Relation.EQ)
			addLeqEdge(new ConstraintEdge(cons, to, source));
		else
			isSymmetric = false;
    }
    
    /**
	 * Adding an inequality to graph
	 * 
	 * @param ieq
	 *            Inequality to be added
	 */
    public void addOneInequality (Inequality ieq) {
    	addOneConstraint(new Constraint(ieq, null, Position.EmptyPosition()));
    }
    
	/**
	 * Add a list of implication rules
	 * 
	 * @param lst A list of implication rules to be added
	 */
	public void addRules(List<Axiom> lst) {
		rules.addAll(lst);
	}
	
	/**
	 * Add a one implication rule
	 * 
	 * @param lst A list of implication rules to be added
	 */
	public void addOneRule(Axiom a) {
		rules.add(a);
	}
    
    /**
     * @return A list of implication rules
     */
    public List<Axiom> getRules() {
		return rules;
	}
    
	/**
	 * Generate a constraint graph from constraints
	 */
    public void generateGraph ( ) {
//        if (generated)
//            return;
		
        /**
         * generate extra nodes and edges for constructors, join and meet elements
         * 1. Constructor edges between a constructor and its parameters
         * 2. Edges from components to a join element
         * 3. Edges from a meet element to components
         */		
        List<Element> workingList = new ArrayList<Element>(eleToNode.keySet());
        Set<Element> processed = new HashSet<Element>();
        
        while (workingList.size()!=0) {
        	Element e = workingList.get(0);
        	Node currentnode = getNode(e);
            workingList.remove(0);
            processed.add(e);
            
            // generate the source node
            Collection<Element> compset;
            
            if (e instanceof EnumerableElement){
            	EnumerableElement ee = (EnumerableElement) e;
            	compset = ee.getElements();
            	
            	int index=0;
                for (Element element : compset) {
                    Node compnode = getNode(element);
                    index++;
                    // add the component element to the working list if not seen before
                    if (!processed.contains(element) && !workingList.contains(element))
                        workingList.add(element);
                    
                    if (e instanceof MeetElement) {
                    	addLeqEdge(new MeetEdge(currentnode, compnode));
                    }
                    else if (e instanceof JoinElement) {
                    	addLeqEdge(new JoinEdge(compnode, currentnode));
                    }
                    else if (e instanceof Application){
                    	Application ae = (Application)e;
                    	Variance variance = ae.getVariance();
                    	if (ae instanceof ConstructorApplication && !ae.getVariance().equals(Variance.NONE)) {
                        	addConEdge(new ConstructorEdge(new EdgeCondition(((ConstructorApplication)ae).getCons(), index, false, variance), compnode, currentnode));
                        	addConEdge(new ConstructorEdge(new EdgeCondition(((ConstructorApplication)ae).getCons(), index, true, variance), currentnode, compnode));
                    	}
                    	else if (ae instanceof VariableApplication && !ae.getVariance().equals(Variance.NONE)) {
                    		addConEdge(new ConstructorEdge(new EdgeCondition(((VariableApplication)ae).getCons(), index, false, variance), compnode, currentnode));
                    		addConEdge(new ConstructorEdge(new EdgeCondition(((VariableApplication)ae).getCons(), index, true, variance), currentnode, compnode));
                    	}
                    }
                }
            }
        }
        if (USE_OPT)
        	removeDominatedVariables();

        if (OPT_AXIOMS) {
        	List<Axiom> useless = new ArrayList<Axiom>();
        	for (Axiom rule : rules) {
        		for (Inequality ieq : rule.getConclusion()) {
        			boolean used = true;
        			if (!ieq.getFirstElement().hasVars() && !ieq.getFirstElement().hasQVars()) {
        				used = false;
            			for (Node n : allNodes) {
            				if (n.getElement().getBaseElement().equals(ieq.getFirstElement())) {
            					used = true;
            					break;
            				}
            			}	
        			}
        			if (used == false) {
        				useless.add(rule);
        				break;
        			}
        			if (!ieq.getSecondElement().hasVars() && !ieq.getSecondElement().hasQVars()) {
        				used = false;
            			for (Node n : allNodes) {
            				if (n.getElement().getBaseElement().equals(ieq.getSecondElement())) {
            					used = true;
            					break;
            				}
            			}	
        			}
        			if (used == false) {
        				useless.add(rule);
        				break;
        			}
        		}
        	}
        	rules.removeAll(useless);
        	if (env != null) {
        		env.setAxioms(rules);
        	}
        }

        // add base elements to the hypothesis graph
        if (env != null)
        	env.addElements(getAllElements());
    }
    
    /**
     * Return a unique neighbor except prev, if such neighbor exists
     * 
     * @param current Current node
     * @param prev Previous node to exclude
     * @return A unique neighbor of current
     */
    private List<Node> getNewOutNodes (Node current, Node prev, List<Node> chain) {
    	if (leqEdges.get(current) == null)
    		return new ArrayList<Node>();
    	List<Node> neighbors = new ArrayList<Node>(leqEdges.get(current).keySet());
    	neighbors.remove(current);
    	neighbors.remove(prev);
    	for (Node n : chain)
    		neighbors.remove(n);
    	return neighbors;
    }
    
    private List<Node> getNewInNodes (Node current, Node prev, List<Node> chain, List<Node> outs, Set<Node> indeg) {
    	if (indeg.size() == 0)
    		return new ArrayList<Node>();
    	List<Node> neighbors = new ArrayList<Node>(indeg);
    	neighbors.remove(prev);
    	neighbors.remove(current);
    	for (Node n : chain)
    		neighbors.remove(n);
    	for (Node n : outs)
    		neighbors.remove(n);
    	return neighbors;
    }
    
    /**
     * Sanity check
     */
    private void checkRemovedNodes () {
    	for (Node node : allNodes) {
    		if (!leqEdges.containsKey(node)) {
				throw new RuntimeException("an node in allNodes is removed from edges");
    		}
    		for (Node n2 : leqEdges.get(node).keySet())
    			if (!allNodes.contains(n2)) {
    				throw new RuntimeException("Node "+node+" still has "+n2);
    			}
    	}
    }
    
    /**
	 * A node n is entry only iff 1. there is only one out edge 2. there are two
	 * out edges, and both of the out nodes are in the innodes
	 * 
	 * @param n
	 * @return
	 */
    private boolean entryOnly (Node n, Set<Node> innodes) {
    	if (leqEdges.get(n).size() == 1)
    		return true;
    	if (leqEdges.get(n).size() == 2) {
    		for (Node in : innodes) {
    			if (!leqEdges.get(n).keySet().contains(in))
    				return false;
    		}
    		return true;
    	}
    	return false;
    }
    
    /**
	 * A node n is entry only iff 1. there is only no in edge other than that
	 * already in chain 2. there is one in edge, other than that in chain, and
	 * it's the only way out
	 * 
	 * @param n
	 * @return
	 */
    private boolean exitOnly (Node n, List<Node> chain, List<Node> innodes) {
    	if (innodes.size() == 0)
    		return true;
    	if (innodes.size() == 1) {
    		Node from = innodes.iterator().next();
    		if (getNewOutNodes(n, from, chain).isEmpty())
    			return true;
    	}
    	return false;
    }
    
    /**
	 * Combine two variables A and B if they show up in the following pattern:
	 * in -- A -- B -- out
	 */
    private void removeDominatedVariables () {
    	int nodesBefore = allNodes.size();

    	Set<Node>[] indeg = new Set[nodesBefore];
    	Set<Node>[] incon = new Set[nodesBefore];
    	for (int i=0; i<nodesBefore; i++) {
    		indeg[i] = new HashSet<Node>();
    		incon[i] = new HashSet<Node>();
    	}
    	for (Node n1 : allNodes) {
    		for (Node n2 : allNodes) {
    			if (!n1.equals(n2) && hasLeqEdge(n1, n2))
    				indeg[n2.getIndex()].add(n1);
    			if (!n1.equals(n2) && hasConEdge(n1, n2))
					incon[n2.getIndex()].add(n1);
			}
    	}
    	
    	boolean modified;
    	do {    	
        	List<Node> chain = new ArrayList<Node>();
        	modified = false;
			
        	for (Node n1 : allNodes) {    	
				// the first node must be "entrance only", meaning that either
				// it has a single out edge, edge to the next node in chain, or
        		// it has two out edges, and one of them is the only way to get in
        		if (!(n1.getElement() instanceof Variable))
        			continue;
        		
        		if (!entryOnly(n1, indeg[n1.getIndex()]) || !incon[n1.getIndex()].isEmpty() 
						|| !conEdges.get(n1).keySet().isEmpty())
        			continue;
        		
//        		if (indeg[n1.getIndex()].size()==1)
//        			continue;
        		
        		Map<Node, Edge> outs = leqEdges.get(n1);
        		for (Node n2 : outs.keySet()) {
					// identify a chain of variables n1 -- n2 --
					// ... -- nm -- .. such that all of n1 to nm have at most 1
					// unique neighbor besides the previous node and itself
        			if (n2.equals(n1))
        				continue;
            		chain.clear();
            		chain.add(n1);

					Node next = n2, prev = n1;
					List<Node> outNodes = getNewOutNodes(next, prev, chain);
					if (outNodes.size()>1)
						continue;
					List<Node> inNodes = getNewInNodes(next, prev, chain, outNodes, indeg[next.getIndex()]);
					while ( outNodes.size() <= 1 && (next.getElement() instanceof Variable)
							&& (chain.isEmpty() || inNodes.isEmpty()) && incon[next.getIndex()].isEmpty() 
							&& conEdges.get(next).keySet().isEmpty()) {
//							&& (indeg[next.getIndex()].size() == 1 || //false)) {
//							(indeg[next.getIndex()].size() == 2 && outNodes.size() == 1 && 
//							edges.get(outNodes.get(0)).containsKey(next)))) {
						chain.add(next);
						prev = next;
						if (outNodes.size() == 1)
							next = outNodes.get(0);
						else {
							break;
						}
						outNodes = getNewOutNodes(next, prev, chain);
						inNodes = getNewInNodes(next, prev, chain, outNodes, indeg[next.getIndex()]);
					}
					// the last node should have no incoming edges except that from the chain
					if (outNodes.size() != 0 && exitOnly(next, chain, inNodes)
							&& incon[next.getIndex()].isEmpty() 
							&& conEdges.get(next).keySet().isEmpty())
						chain.add(next);

					if (chain.size() > 1) {
						modified = true;
						break;
					}
				}
        		if (modified)
        			break;
        	}

        	// remove nodes
        	if (chain.size() > 1) {
//        		Node last=chain.get(chain.size()-1);
//        		for (Node node : chain) {
//        			System.out.print(node.getElement().toDetailString()+ "-->");
//        		}
//        		System.out.println("");
        		collapse(chain, indeg, eleToNode);
//				checkRemovedNodes();
			}
    	}
    	while (modified);
    	
    	// fix indices
    	int count = 0;
    	for (Node n : allNodes) {
    		n.setIndex(count);
    		count ++;
    	}
    	
    	int nodesAfter = allNodes.size();
    	if (nodesAfter < nodesBefore)
    		System.out.println("[Remove dominated] Reduced node size from " + nodesBefore + " to " + nodesAfter);
    }
    
	/**
	 * Links from node to all neighbors in graph in DOT format
	 * 
	 * @param node
	 *            A graph node
	 * @return A DOT string representing the links from <code>node</code> to all
	 *         neighbors in graph
	 */
    private String printLinkToDotString (Node node) {
        String ret = "";
        Set<Node> neighbors = getNeighbors(node);
        for (Node n : neighbors) {
			for (Edge edge : getEdges(node, n)) {
				if (n.shouldPrint()) {
					if (edge.isDirected())
						ret += node.getUid() + "->" + n.getUid() + " [label=\""
								+ edge.toString() + "\"];\n";
					else if (node.getIndex() < n.getIndex())
						ret += node.getUid() + "->" + n.getUid() + " [dir=both label=\""
								+ edge.toString() + "\"];\n";
				}
			}
        }
        return ret;
    }
    
	/**
	 * @return A string in DOT file format which represents the graph
	 */
    public String toDotString ( ) {
        String ret = "";
        String nodes = "";
        String links = "";
        
        for (Node n : allNodes) {
			if (!n.shouldPrint())
				continue;
			nodes += n.toDotString();
			links += printLinkToDotString(n);
        }
        
        ret += "digraph G1 {\n";
        // print source code
        if (PRINT_SRC) {
            for (String s : files) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(s));
                    String line = reader.readLine();
                    int linenum = 1;
                    ret += "source [shape=box, label=\"";
                    while (line != null) {                           
                    	ret += linenum + ":\t" + StringUtil.sanitize(line) + "\\l";
                        line = reader.readLine();
                        linenum++;
                    }
                    ret += "\"];\n";
                } catch (IOException e) {
                    continue;
                }
            }
        }
        
        ret += "node [color = grey, style = filled];\n";
        ret += nodes;
        ret += links;
        ret += "}\n";
        return ret;
    }
    
	/**
	 * Mark graph nodes that relate to errors
	 */
    public void slicing () {
    	for (Node node : allNodes) {
    		if (node.isCause())
    			node.markAsPrint();
    	}
    }
    
	/**
	 * @return All constraint elements
	 */
    public Set<Element> getAllElements () {
     return eleToNode.keySet();	
    }
        
    /**
     * @return Global assumptions
     */
    public Hypothesis getEnv() {
		return env;
	}
    
    /** 
     * @return True if all constraints are symmetric (only equalities)
     */
    public boolean isSymmetric() {
		return isSymmetric;
	}
}
