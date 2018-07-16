package sherrloc.constraint.ast;

import java.util.List;

import java_cup.internal_error;

import sherrloc.util.PrettyPrinter;

/**
 * Inequality is the building block of constraints. An inequality has the format
 * of {@link Relation} (e1 , e2), where {@link Relation} can be <= or ==
 */
public class Inequality implements PrettyPrinter {
	protected final Element e1, e2;
	protected final Relation r;

	/**
	 * Builds an inequality <code>r(e1, e2)</code>
	 */
	public Inequality(Element e1, Element e2, Relation r) {
		this.e1 = e1;
		this.e2 = e2;
		this.r = r;
	}

	/**
	 * @return Element <code>e1</code> in constraint <code>assumption</code>
	 *         entails <code>r(e1,e2)</code>
	 */
	public Element getFirstElement() {
		return e1;
	}

	/**
	 * @return Element <code>e2</code> in constraint <code>assumption</code>
	 *         entails <code>r(e1,e2)</code>
	 */
	public Element getSecondElement() {
		return e2;
	}

	/**
	 * @return Relation (<= or ==)
	 */
	public Relation getRelation() {
		return r;
	}

	/**
	 * @return All variables used in the constraint
	 */
	public List<Variable> getVars() {
		List<Variable> ret = e1.getVars();
		ret.addAll(e2.getVars());
		return ret;
	}

	public Inequality baseInequality() {
		return new Inequality(e1.getBaseElement(), e2.getBaseElement(), r);
	}

	@Override
	public String toString() {
		return e1.toString() + " " + r.toString() + " " + e2.toString();
	}

	@Override
	public String toHTMLString() {
		String e1s = ((e1 instanceof Variable)?"(variable)":"")+e1.toString();
		String e2s = ((e2 instanceof Variable)?"(variable)":"")+e2.toString();
		if (r == Relation.EQ)
			return ("<code>" + e1s + " == " + e2s + "</code>");
		else
			return ("<code>" + e1s + " <= " + e2s + "</code>");
	}

	@Override
	public String toConsoleString() {
		return e1.toSnippetString() + r.toString() + e2.toSnippetString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Inequality) {
			Inequality ieq = (Inequality) obj;
			return e1.equals(ieq.e1) && e2.equals(ieq.e2) && r.equals(ieq.r);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return e1.hashCode() * 4759 + e2.hashCode() * 523
				+ ((r == Relation.EQ) ? 1 : 0);
	}
}
