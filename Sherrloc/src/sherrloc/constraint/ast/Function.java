package sherrloc.constraint.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sherrloc.graph.Variance;

/**
 * A function maps constraint elements to a single element. It has a name, an arity
 */
public class Function extends Element {
	private int arity;
	
	/**
	 * @param name Function name
	 * @param arity Arity of the functions
	 * @param p Position of the element in source code
	 */
	public Function(String name, int arity, Position p) {
		super(name, p);
		this.arity = arity;
	}
	
	/**
	 * @return Arity of the function
	 */
	public int getArity () {
		return arity;
	}
	
	/**
	 * Set arity of a function (used only in varMode, where functions are inferred from the constraint file)
	 */
	public void setArity(int a) {
		arity = a;
	}
	
	/**
	 * @return Return the variance of all parameters (all parameters must have
	 *         the same variance in the current implementation)
	 */
	public Variance getVariance () {
		return Variance.NONE;
	}
	
	@Override
	public String toString () {
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
	 * Same function used at different positions are treated as different elements to improve the precision of error diagnosis
	 */
	public boolean equals(Object o) {
		if (o instanceof Function) {
			Function c = (Function)o;
			return arity==c.arity && this.name.equals(c.name) && this.pos.equals(c.pos);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return arity * 85751 + name.hashCode()*2313 + pos.hashCode();
	}
		
	@Override
	public Function clone ( ) {
		return new Function(name, arity, pos);
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
		return new Function(name, arity, Position.EmptyPosition());
	}	
	
	@Override
	public boolean unifyWith(Element e, Map<QuantifiedVariable, Element> map) {
		return equals(e);
	}
	
	@Override
	public Element subst(Map<QuantifiedVariable, Element> map) {
		return this;
	}
	
	@Override
	public boolean matches(Element e, Map<Variable, Element> map) {
		return equals(e);
	}
}
