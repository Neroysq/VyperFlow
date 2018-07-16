package sherrloc.constraint.analysis;

import java.util.List;
import java.util.Set;

import sherrloc.graph.ConstraintGraph;
import sherrloc.graph.Edge;
import sherrloc.graph.Node;

/**
 * The interface for path finders, which saturates a constraint graph so that
 * derivable partial orderings from constraints represented in graph are
 * represented in the saturated graph
 */
public interface PathFinder {

	/**
	 * Return a path in the constraint graph, when a partial ordering on
	 * <code>start</code> and <code>end</code> is derivable from all constraints
	 * along the returned path
	 * 
	 * @param start
	 *            Node on LHS
	 * @param end
	 *            Node on RHS

	 * @return A constraint path such that <code>start <= end</code> is
	 *         derivable from constraints along the path. Return null if no such
	 *         path exists
	 */
	public List<Edge> getPath(Node start, Node end);
	
	/**
	 * @return True if an LEQ edge can be inferred on the end nodes
	 */
	public boolean hasLeqEdge (Node from, Node end);
	
	/**
	 * @return A set of nodes such that an LEQ edge node can be inferred between from and them
	 */
	public Set<Node> getFlowsTo (Node from); 
	
	/**
	 * @return A set of nodes such that an LEQ edge node can be inferred between them and to
	 */
	public Set<Node> getFlowsFrom (Node to);
	
	/**
	 * @return Length of an LEQ edge inferred on the end nodes
	 */
	public int leqEdgeLength (Node from, Node end);
	
	/**
	 * @return True if an LEFT edge can be inferred on the end nodes
	 */
	public boolean hasLeftEdge (Node from, Node end);
	
	/**
	 * Return all paths in the constraint graph, when there a LEFT edge on
	 * <code>start</code> and <code>end</code> is derivable from all constraints
	 * along the returned paths
	 * 
	 * @param start
	 *            Node on LHS
	 * @param end
	 *            Node on RHS
	 * 
	 * @return Constraint paths such that <code>start LEFT end</code> is
	 *         derivable from constraints along the paths. Empty if no such
	 *         path exists
	 */
	public List<List<Edge>> getLeftPaths(Node start, Node end);
	
	/**
	 * @return Constraint graph to be saturated
	 */
	public ConstraintGraph getGraph ();
}
