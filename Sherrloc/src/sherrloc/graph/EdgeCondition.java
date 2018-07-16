package sherrloc.graph;

import sherrloc.constraint.ast.Constructor;
import sherrloc.constraint.ast.Element;
import sherrloc.constraint.ast.Variable;


/**
 * Information needed for an edge representing constructors, including
 * constructor, index of the parameter, and variance.
 */
public class EdgeCondition {
	private final Element con;
	private final int index;
	private final boolean reverse;
	private final Variance variance;
	
	/**
	 * @param con
	 *            The concrete constructor being applied to
	 * @param index
	 *            The index of the parameter, in the constructor, that the edge
	 *            is connecting to
	 * @param reverse
	 *            True if the edge is from constructor to a parameter
	 * @param v
	 *            Variance of the parameter
	 */
	public EdgeCondition(Constructor con, int index, boolean reverse, Variance v) {
		this.con = con;
		this.index = index;
		this.reverse = reverse;
		this.variance = v;
	}
	
	/**
	 * @param var
	 *            The constructor variable to be applied to
	 * @param index
	 *            The index of the parameter, in the constructor, that the edge
	 *            is connecting to
	 * @param reverse
	 *            True if the edge is from constructor to a parameter
	 * @param v
	 *            Variance of the parameter
	 */
	public EdgeCondition(Variable var, int index, boolean reverse, Variance v) {
		this.con = var;
		this.index = index;
		this.reverse = reverse;
		this.variance = v;
	}
	
	/**
	 * @return True if the edge is from constructor to a parameter
	 */
	public boolean isReverse () {
		return reverse;
	}
	
	/**
	 * @return A {@link EdgeCondition} that matches this condition
	 */
	public EdgeCondition getMatch () {
		if (con instanceof Constructor)
			return new EdgeCondition((Constructor)con, index, !reverse, variance);
		else 
			return new EdgeCondition((Variable)con, index, !reverse, variance);
	}
	
	/**
	 * @return Variance
	 */
	public Variance getVariance() {
		return variance;
	}
	
	/**
	 * @return Constructor
	 */
	public Element getCon() {
		return con;
	}
	
	@Override
	public String toString () {
		if (reverse)
			return con.toString()+"@"+index+"^(-1)";
		else
			return con.toString()+"@"+index;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EdgeCondition) {
			EdgeCondition ec = (EdgeCondition) obj;
			return con.equals(ec.con) && index == ec.index 
				&& reverse == ec.reverse && variance == ec.variance; 
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return con.hashCode() * 1237 + index * 131 + variance.ordinal()*5 + (reverse?1:0);
	}
	
	/**
	 * Return true if the current condition matches the parameter. That is, if
	 * <UL>
	 * <LI>They have the same constructor, or at least one of them is a variable
	 * <LI>They have the same index
	 * <LI>Only one of them is reverse
	 * </UL>
	 * 
	 * @param c
	 *            {@link EdgeCondition}
	 * 
	 * @return True if current condition matches parameter
	 */
	public boolean matches (EdgeCondition c) {
		boolean sameCons = false;
		if (con instanceof Variable || c.con instanceof Variable )
			sameCons = true;
		else {
			// both constructors are concrete
			sameCons = con.equals(c.con);
		}
		return (sameCons && index==c.index && logicXOR(reverse,c.reverse));
	}
	
	/**
	 * @return Logical XOR of two parameters
	 */
	private boolean logicXOR (boolean a, boolean b) {
		return (!(a&&b) && (a||b));
	}
}
