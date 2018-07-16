package sherrloc.diagnostic.explanation;

import sherrloc.constraint.ast.Constraint;
import sherrloc.graph.ConstraintEdge;
import sherrloc.graph.ConstraintPath;
import sherrloc.graph.Edge;

/**
 * A basic unit of constraint explanation
 */
public class ConstraintEntity extends Entity {
	final private String pos;
	final private String html;
	final private String console;
	
	/**
	 * @param cons A constraint
	 * @param succ # satisfiable paths using the constraint
	 */
	public ConstraintEntity(String pos, String html, String console, int succ) {
		super(succ);
		this.pos = pos;
		this.html = html;
		this.console = console;
	}
	
	@Override
	public boolean explains(ConstraintPath p) {
		for (Edge edge : p.getEdges()) {
			if (edge instanceof ConstraintEdge && ((ConstraintEdge) edge).getConstraint().getPos().toString().equals(pos)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void toHTML(StringBuffer locBuf, StringBuffer expBuf) {
		locBuf.append("['left', \'"+pos+"\'], ");
		expBuf.append(html + " [loc: " + pos + "]    ");
	}
	
	@Override
	public void toConsole(StringBuffer locBuf, StringBuffer expBuf) {
		locBuf.append(pos + ", ");
		expBuf.append(console + ", ");
	}

	@Override
	public String toString() {
		return console + pos;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ConstraintEntity) {
			return pos.equals(((ConstraintEntity) other).pos);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return pos.hashCode();
	}
}
