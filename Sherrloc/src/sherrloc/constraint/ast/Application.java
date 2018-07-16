package sherrloc.constraint.ast;

import java.util.ArrayList;
import java.util.List;

import javax.sql.PooledConnection;

import sherrloc.graph.Variance;

/**
 * This class represents an application to a constructor, possibly with no
 * parameters (e.g., list int, int)
 * 
 * The constructor can either be concrete {@link Constructor}, or abstract
 * {@link Variable}
 */
public abstract class Application extends EnumerableElement {

	public Application(String name, List<Element> elements) {
		super(name, elements);
	}
	
	/**
	 * @return True if the parameters are contravariant
	 */
	public abstract Variance getVariance();
	
	/**
	 * Return a (all) new applications where e1 is replaced with e2. For instance, '(String, String)'.replace('String', 'Name')' should return: 
	 * ['(Name, String)', '(String, Name)', '(Name, Name)']
	 */
	public abstract List<Application> replace (Element e1, Element e2);
	
	/**
	 * A help method that takes a list of elements to be replaced (elements), and updates all possible replacements so far (ret)
	 * 
	 * @param elements A list of elements to be replaced
	 * @param idx Current index of elements to work on; starting from 0
 	 * @param e1 Element to be replaced
	 * @param e2 Element to replace e1
	 * @param ret All possible replacements so far
	 */
	protected void _replace(List<Element> elements, int idx, Element e1, Element e2, List<List<Element>> ret) {
		if (idx > elements.size()-1)
			return;
		Element ei = elements.get(idx);
		// collect all possible replacements of ei
		List<Element> possiblePlacements = new ArrayList<Element>();
		if (ei.equals(e1)) {
			// option 1: replace e1 with e2
			possiblePlacements.add(e2);
		}
		else if (ei instanceof Application) {
			// nested applications
			Application app = (Application) ei;
			List<Application> replacements = app.replace(e1, e2);
			for (Application replacement : replacements) {
				possiblePlacements.add(replacement);
			}
		}
		
		List<List<Element>> toadd = new ArrayList<List<Element>>();
		for (List<Element> sofar : ret) {
			// option 1: replace the current element ei
			for (Element replacement : possiblePlacements) {
				List<Element> newlst = new ArrayList<Element>();
				newlst.addAll(sofar);
				newlst.add(replacement);
				toadd.add(newlst);
			}
			
			// option 2: no replacement
			sofar.add(ei);
		}
		
		for (Element replacement : possiblePlacements) {
			List<Element> newlst = new ArrayList<Element>();
			newlst.addAll(elements.subList(0, idx));
			newlst.add(replacement);
			toadd.add(newlst);
		}
		
		ret.addAll(toadd);
		// recursively replace the remaining of elements
		_replace(elements, idx+1, e1, e2, ret);
	}
}
