package sherrloc.constraint.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sherrloc.constraint.analysis.Evidence;
import sherrloc.constraint.analysis.PathFinder;
import sherrloc.graph.ConstraintGraph;
import sherrloc.graph.LeqCondition;
import sherrloc.graph.LeqEdge;
import sherrloc.graph.Node;

/**
 * Axioms have the form of: \forall x,y. C_1 => C_2, where C is a conjunction of
 * inequalities
 * 
 */
public class Axiom {
	List<QuantifiedVariable> qvars;		// quantified variables
	Set<Inequality> premise;
	Set<Inequality> conclusion;
	
	public Axiom(List<QuantifiedVariable> vars, Set<Inequality> ieq1, Set<Inequality> ieq2) {
		qvars = vars;
		premise = ieq1;
		conclusion = ieq2;
	}
	
	private class NodeMatch {
		private Node n;
		private Map<QuantifiedVariable, Element> map;
		
		public NodeMatch(Node n, Map<QuantifiedVariable, Element> map) {
			this.n = n;
			this.map = map;
		}
	}
	
	public class EdgeMatch {
		public Node n1;
		public Node n2;
		public Map<QuantifiedVariable, Element> map;
		
		public EdgeMatch(Node n1, Node n2, Map<QuantifiedVariable, Element> map) {
			this.n1 = n1;
			this.n2 = n2;
			this.map = map;
		}
	}
	
	public class PremiseMatch {
		public Map<QuantifiedVariable, Element> map;
		public int size;
		public List<Evidence> evidences;
		public boolean noGrayNodes;
		
		public PremiseMatch(Map<QuantifiedVariable, Element> map, int size, List<Evidence> evis, boolean noGrayNodes) {
			this.map = map;
			this.size = size;
			this.evidences = evis;
			this.noGrayNodes = noGrayNodes;
		}
	}
	
	public void addQVar (QuantifiedVariable v) {
		qvars.add(v);
	}
	
	public void addPremise (Inequality ieq) {
		premise.add(ieq);
	}
	
	public void addConclusion (Inequality ieq) {
		conclusion.add(ieq);
	}
	
	public Set<Inequality> getConclusion() {
		return conclusion;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (!qvars.isEmpty()) {
			sb.append("\\");
			for (QuantifiedVariable var : qvars) {
				sb.append(var + " ");
			}
			sb.append(". ");
		}
		int count = 1;
		for (Inequality ieq : premise) {
			if (count > 1)
				sb.append(" ∧ ");
			sb.append(ieq);
			count ++;
		}
		sb.append(" => ");
		count = 1;
		for (Inequality ieq : conclusion) {
			if (count > 1)
				sb.append(" ∧ ");
			sb.append(ieq);
			count ++;
		}
		return sb.toString();
	}
	
	/**
	 * A quick test if a new LEQ edge in the constraint graph would match the axiom
	 * 
	 * @return True if edge matches at least one promise of the axiom
	 */
	public boolean mayMatch (LeqEdge edge) {
		if (premise.isEmpty())
			return true;
		
		for (Inequality ieq : premise) {
			Map<QuantifiedVariable, Element> map1 = new HashMap<QuantifiedVariable, Element>();
			Map<QuantifiedVariable, Element> map2 = new HashMap<QuantifiedVariable, Element>();
			if (ieq.e1.unifyWith(edge.getFrom().getElement(), map1) && 
					ieq.e2.unifyWith(edge.getTo().getElement(), map2))
				return true;
		}
		return false;
	}
		
	/**
	 * Return all edges in the graph whose end nodes can potentially be unified
	 * with e1 and e2. Update substitutions made in the unification when
	 * necessary.
	 * 
	 * @param finder
	 *            A saturated constraint graph where extra edges are to be
	 *            inferred from axioms
	 * @param maps
	 *            All possible substitutions so far
	 * @return All edges in the graph whose end nodes can potentially be unified
	 *         with e1 and e2. Parameter maps is updated to remove infeasible
	 *         substitutions, and make new substitutions for the axiom when
	 *         necessary
	 */
	public List<EdgeMatch> findMatches (Element e1, Element e2, PathFinder finder, Map<QuantifiedVariable, Element> map) {		
		// try to unify one promise at one time
		List<EdgeMatch> ret = new ArrayList<EdgeMatch>();

		List<NodeMatch> match1 = matchOneNode(e1, map, finder.getGraph());
		// get a pair of nodes, and see if they matches the promise
		for (NodeMatch m1 : match1) {
			List<NodeMatch> match2 = matchOneNode(e2, m1.map, finder.getGraph());
			for (NodeMatch m2 : match2) {
				ret.add(new EdgeMatch(m1.n, m2.n, m2.map));
			}
		}
		return ret;
	}
	
	public List<PremiseMatch> findMatchesInPremise (PathFinder finder) {		
		List<PremiseMatch> matches = new ArrayList<PremiseMatch>();
		
		matches.add(new PremiseMatch(new HashMap<QuantifiedVariable, Element>(), 0, new ArrayList<Evidence>(), true)); // add an empty
																// substitution
		for (Inequality ieq : premise) {
			List<PremiseMatch> newmatches = new ArrayList<PremiseMatch>();
			while (!matches.isEmpty()) {
				PremiseMatch pmatch = matches.remove(0);
				List<EdgeMatch> lst = findMatches(ieq.e1, ieq.e2, finder, pmatch.map);
				boolean noGray = pmatch.noGrayNodes;

				for (EdgeMatch match : lst) {
					Node n1 = match.n1, n2 = match.n2;
					boolean noGrayNodes = noGray && !n1.isGray() && !n2.isGray();
					int size = pmatch.size;
					List<Evidence> evidences = new ArrayList<Evidence>(pmatch.evidences); 	// track evidences that lead to the inference
					if ((finder.hasLeqEdge(n1, n2) && (ieq.r != Relation.EQ || finder.hasLeqEdge(n2, n1)))) {
						size += finder.leqEdgeLength(n1, n2);
						evidences.add(new Evidence(n1, n2, LeqCondition.getInstance()));
						
						if (ieq.r == Relation.EQ && finder.hasLeqEdge(n2, n1)) {
							size += finder.leqEdgeLength(n2, n1);
							evidences.add(new Evidence(n2, n1, LeqCondition.getInstance()));
						}
						newmatches.add(new PremiseMatch(match.map, size, evidences, noGrayNodes));
					}
				}
			}
			matches = newmatches;
		}
		
		return matches;
	}
	
	/**
	 * Return a list of nodes in the graph that matches an element, which in
	 * general, may contain quantified variables
	 * 
	 * @param e
	 *            An element to be matched
	 * @param m
	 *            Substitutions collected so far
	 * @return A list of nodes that matches element
	 */
	private List<NodeMatch> matchOneNode (Element e, Map<QuantifiedVariable, Element> m, ConstraintGraph g) {
		List<NodeMatch> ret = new ArrayList<NodeMatch>();

		if (!e.hasQVars()) {
			if (g.getMatchedNodes(e) == null)
				return ret;
			for (Node node : g.getMatchedNodes(e)) {
				ret.add(new NodeMatch(node,m));
			}
		} else {
			// complex case. Need to try all nodes in the graph
			for (Node n : g.getAllNodes()) {
				Map<QuantifiedVariable, Element> map = new HashMap<QuantifiedVariable, Element>();
				map.putAll(m);
				if (e.unifyWith(n.getElement(), map)) {
					ret.add(new NodeMatch(n, map));
				}
			}
		}
		return ret;
	}
}
