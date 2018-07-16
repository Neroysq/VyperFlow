package sherrloc.constraint.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sherrloc.graph.Variance;

/**
 * A constructor (e.g., list(1), pair(2)) has a name, an arity and the variance
 * of parameters
 */
public class Constructor extends Element {
	private int arity;
	private final Variance variance;
	private final int level;
	
	/**
	 * @param name Constructor name
	 * @param arity Arity of the constructor
	 * @param variance The variance of all parameters
	 * @param p Position of the element in source code
	 */
	public Constructor(String name, int arity, int level, Variance variance, Position p) {
		super(name, p);
		this.arity = arity;
		this.variance = variance;
		this.level = level;
	}
	
	/**
	 * @return Arity of the constructor
	 */
	public int getArity () {
		return arity;
	}
	
	/**
	 * Set arity of a constructor (used only in varMode, where constructors are inferred from the constraint file)
	 */
	public void setArity(int a) {
		arity = a;
	}
	
	/**
	 * @return Return the variance of all parameters (all parameters must have
	 *         the same variance in the current implementation)
	 */
	public Variance getVariance () {
		return variance;
	}
	
	@Override
	public String toString () {
		if (name.equals("arrow"))
			return "->";
		if (name.equals("larrow"))
			return "<-";
		else if (name.equals("pair"))
			return "*";
		else
			return name;
	}
			
	@Override
	public String toSnippetString () {
		if (!pos.isEmpty())
			return pos.getSnippet();
		else
			return toString();
	}
	
	@Override
	public String toDotString () {
		return toString();
	}
		
	@Override
	public List<Variable> getVars () {
		return new ArrayList<Variable>();
	}
	
	@Override
	public boolean hasVars() {
		return false;
	}
	
	@Override
	public boolean hasQVars() {
		return false;
	}
	
	@Override
	/**
	 * Same constructor used at different positions are treated as different elements to improve the precision of error diagnosis
	 */
	public boolean equals(Object o) {
		if (o instanceof Constructor) {
			Constructor c = (Constructor)o;
			return arity==c.arity && this.name.equals(c.name) && this.variance ==c.variance && this.pos.equals(c.pos);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return arity * 85751 + name.hashCode()*1913 + pos.hashCode()*3 + (this.variance.hashCode());
	}
		
	@Override
	public Constructor clone ( ) {
		return new Constructor(name, arity, level, variance, pos);
	}
	
	@Override
	public boolean isBottom() {
		return false;
	}
	
	@Override
	public boolean isTop() {
		return false;
	}
	
	@Override
	public boolean trivialEnd() {
		return false;
	}
	
	@Override
	public Element getBaseElement() {
		return new Constructor(name, arity, level, variance, Position.EmptyPosition());
	}	
	
	@Override
	public boolean unifyWith(Element e, Map<QuantifiedVariable, Element> map) {
		if (e instanceof QuantifiedVariable)
			return e.unifyWith(this, map);
		else return equals(e);
	}
	
	@Override
	public Element subst(Map<QuantifiedVariable, Element> map) {
		return this;
	}
	
	@Override
	public boolean matches(Element e, Map<Variable, Element> map) {
		if (e instanceof Variable)
			return e.matches(this, map);
		else return equals(e);
	}
	
	@Override
	public int getSkolemLevel() {
		return level;
	}
}
