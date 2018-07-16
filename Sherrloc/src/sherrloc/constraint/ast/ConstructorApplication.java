package sherrloc.constraint.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sherrloc.graph.Variance;

/**
 * This class represents an application of a {@link Constructor}, possibly with
 * no parameters (e.g., list int, int)
 */
public class ConstructorApplication extends Application {
	private final Constructor cons;
	private ConstructorApplication baseelem = null;
	private boolean bot, top;
	private int level=0;
	
	/**
	 * @param cons A constructor
	 * @param elements Parameters applied to the constructor <code>cons</code>
	 */
	public ConstructorApplication(Constructor cons, List<Element> elements) {
		super("", elements);
		this.cons = cons;
		this.top = _isTop();
		this.bot = _isBottom();
		for (Element e : elements) {
			if (level < e.getSkolemLevel()) {
				level = e.getSkolemLevel();
			}
		}
	}
	
	/**
	 * @return Constructor being applied to
	 */
	public Constructor getCons() {
		return cons;
	}
	
	@Override
	public void setPosition(Position pos) {
		super.setPosition(pos);
		if (pos != Position.EmptyPosition()) {
			for (Element ele : elements) {
				if (ele.getPosition() == Position.EmptyPosition())
					ele.setPosition(pos);
			}
		}
	}
	
	@Override
	String getSymbol() {
		return cons.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConstructorApplication) {
			ConstructorApplication ce = (ConstructorApplication) o;
			if (pos.equals(ce.pos) && cons.equals(ce.cons)) {
				if (ce.getElements().size() == elements.size()) {
					for (int i = 0; i < elements.size(); i++) {
						if (!elements.get(i).equals(ce.getElements().get(i)))
							return false;
					}
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int ret = cons.hashCode()*941;
		for (Element e : elements) {
			ret += e.hashCode()*13;
		}
		return ret+pos.hashCode();
	}
	
	/**
	 * Special handling is need to lift the "topness". For example, in Jif,
	 * Top->Top, where Top is the top element in the lattice of principals, is
	 * the top element in the lattice of labels
	 */
	@Override
	public boolean isTop() {
		return top;
	}
	
	public boolean _isTop() {
		for (Element e : elements) {
			if (cons.getVariance().equals(Variance.POS) && !e.isTop())
				return false;
			if (cons.getVariance().equals(Variance.NEG) && !e.isBottom())
				return false;
			if (cons.getVariance().equals(Variance.NONE))
				return false;
		}
		return true;
	}
	
	/**
	 * Similar to {@link #isTop()}
	 */
	@Override
	public boolean isBottom() {
		return bot;
	}
	
	public boolean _isBottom() {
		for (Element e : elements) {
			if (cons.getVariance().equals(Variance.POS) && !e.isBottom())
				return false;
			if (cons.getVariance().equals(Variance.NEG) && !e.isTop())
				return false;
			if (cons.getVariance().equals(Variance.NONE))
				return false;
		}
		return true;
	}
	
	@Override
	public boolean trivialEnd() {
		return false;
	}
		
	@Override
	public Element getBaseElement() {
		if (baseelem != null)
			return baseelem;
		
		List<Element> baseElements =  new ArrayList<Element>();
		for (Element e : elements) {
			baseElements.add(e.getBaseElement());
		}
		baseelem = new ConstructorApplication((Constructor)cons.getBaseElement(), baseElements);
		return baseelem;
	}
	
	@Override
	public Variance getVariance() {
		return cons.getVariance();
	}
	
	@Override
	public boolean unifyWith(Element e, Map<QuantifiedVariable, Element> map) {
		if (e instanceof VariableApplication)
			return e.unifyWith(this, map);
		if (e instanceof ConstructorApplication) {
			ConstructorApplication ca = (ConstructorApplication) e;
			if (!cons.unifyWith(ca.getCons(), map))
				return false;
			return super.unifyWith(e, map);
		}
		return false;
	}
	
	@Override
	public Element subst(Map<QuantifiedVariable, Element> map) {
		return new ConstructorApplication(cons, substElements(map));
	}
	
	@Override
	public boolean matches(Element e, Map<Variable, Element> map) {
		if (e instanceof VariableApplication)
			return e.matches(this, map);
		if (e instanceof ConstructorApplication) {
			ConstructorApplication ca = (ConstructorApplication) e;
			if (!cons.matches(ca.getCons(), map))
				return false;
			return super.matches(e, map);
		}
		return false;
	}
	
	@Override
	public List<Application> replace(Element e1, Element e2) {
		List<List<Element>> replacements =  new ArrayList<List<Element>>();
		List<Application> ret = new ArrayList<Application>();
		_replace(elements, 0, e1, e2, replacements);
		for (List<Element> lst : replacements) {
			ret.add(new ConstructorApplication(cons, lst));
		}
		return ret;
	}
	
	@Override
	public int getSkolemLevel() {
		return level;
	}
}
