package sherrloc.graph;

import java.util.Set;

import sherrloc.constraint.analysis.CFLPathFinder;
import sherrloc.constraint.ast.Inequality;


/**
 * Derived edges that only used in saturated constraint graphs (used in
 * CFL-reachability algorithm, see {@link CFLPathFinder})
 */
abstract public class ReductionEdge extends Edge{
	protected final int size;
	
	/**
	 * @param first
	 *            First edge based on which the constructed edge is derived from
	 * @param second
	 *            Second edge based on which the constructed edge is derived
	 *            from
	 */
	public ReductionEdge(Node from, Node to, int size) {
		super(from, to);
		this.size = size;
	}
	
	@Override
	public boolean isDirected() {
		return true;
	}
	
	@Override
	public String toString() {
		return "reduction";
	}
		
	@Override
	public int getLength () {
		return size;
	}
	
	@Override
	public Set<Inequality> getHypothesis() {
		return null;
	}
}
