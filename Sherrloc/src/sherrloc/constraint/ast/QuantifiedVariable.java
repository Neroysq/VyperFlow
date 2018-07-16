package sherrloc.constraint.ast;

import java.util.Map;

public class QuantifiedVariable extends Variable {
	
	public QuantifiedVariable(String name) {
		super(name, 0);
	}
	
	@Override
	public boolean hasQVars() {
		return true;
	}
	
	@Override
	public boolean unifyWith(Element e, Map<QuantifiedVariable, Element> map) {
		if (map.containsKey(this))
			return map.get(this).unifyWith(e, map);
		else {
			map.put(this, e);
			return true;
		}
	}
	
	@Override
	public Element subst(Map<QuantifiedVariable, Element> map) {
		if (map.containsKey(this))
			return map.get(this);
		else
			return this;
	}
}
