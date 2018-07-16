package sherrloc.graph;

import sherrloc.constraint.ast.Variable;

/**
 * A singleton class that represents the empty condition on LEQ edges
 */
public class LeqCondition extends EdgeCondition {
	private static LeqCondition instance = null;
	
	private LeqCondition() {
		super(new Variable("",0), 0, false, null);
	}
	
	public static LeqCondition getInstance () {
		if (instance==null)
			instance = new LeqCondition();
		return instance;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LeqCondition) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 1137;
	}
	
	@Override
	public boolean matches (EdgeCondition c) {
		return false;
	}
	
	@Override
	public String toString() {
		return "LEQ";
	}
}