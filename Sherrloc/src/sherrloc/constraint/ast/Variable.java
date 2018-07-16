package sherrloc.constraint.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sherrloc.util.StringUtil;

/**
 * This class represents constraint variable to be inferred
 */
public class Variable extends Element {
	private final int level;
	
	/**
	 * @param name Variable name
	 */
	public Variable(String name, int level) {
		super(name, Position.EmptyPosition());
		this.level = level;
	}
	
	public int getVarLevel() {
		return level;
	}
		
	@Override
	public List<Variable> getVars () {
		List<Variable> ret = new ArrayList<Variable>();
		ret.add(this);
		return ret;
	}
	
	@Override
	public boolean hasVars() {
		return true;
	}
	
	@Override
	public boolean hasQVars() {
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Variable) {
			return this.name==((Variable)o).name;
		}
		return false;
	}
		
	@Override
	public String toString() {
		return toSnippetString();
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public String toDotString() {
		return StringUtil.getPrettyName(name);
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
		return true;
	}
	
	@Override
	public Element getBaseElement() {
		return this;
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
		if (map.containsKey(this))
			return map.get(this).matches(e, map);
		else {
			if (e.getVars().contains(this))
				return false;
			map.put(this, e);
			return true;
		}
	}
}
