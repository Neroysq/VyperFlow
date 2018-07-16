package sherrloc.constraint.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import sherrloc.constraint.ast.JoinElement;
import sherrloc.constraint.ast.MeetElement;
import sherrloc.graph.ConstraintEdge;
import sherrloc.graph.ConstraintGraph;
import sherrloc.graph.ConstructorEdge;
import sherrloc.graph.DummyEdge;
import sherrloc.graph.Edge;
import sherrloc.graph.EdgeCondition;
import sherrloc.graph.JoinEdge;
import sherrloc.graph.LeftEdge;
import sherrloc.graph.LeqCondition;
import sherrloc.graph.LeqEdge;
import sherrloc.graph.LeqRevCondition;
import sherrloc.graph.MeetEdge;
import sherrloc.graph.Node;
import sherrloc.graph.ReductionEdge;
import sherrloc.graph.RightEdge;

/**
 * Saturate a constraint graph according to a context-free-grammar with three
 * types of edges:
 * <ul>
 * <li>LEQ: an inequality on node
 * <li>LEFT: constructor edge
 * <li>RIGHT: destructor edge
 * </ul>
 * <p>
 * See the full grammar in the paper "Toward General Diagnosis of Static Errors"
 * by Danfeng Zhang and Andrew C. Myers
 */
abstract public class CFLPathFinder implements PathFinder {
	/** Edges used in CFL-reachablity algorithm */
	protected Map<Integer, Map<Integer, Map<EdgeCondition, List<Evidence>>>> nextHop;
	// since the RIGHT edges are rare in a graph, and no right edges are
	// inferred, using HashMap can be more memory efficient than arrays
	protected Map<Integer, Map<Integer, List<RightEdge>>> rightPath;
	private Map<Integer, Set<Integer>> inferredLR;

	/** other fields */
	protected final ConstraintGraph g;	
	
	public enum EDGE_TYPE {LEQ, LEFT, RIGHT};
	/**
	 * @param graph
	 *            A graph to be saturated
	 */
	public CFLPathFinder(ConstraintGraph graph) {
		g = graph;
		nextHop = new HashMap<Integer, Map<Integer, Map<EdgeCondition, List<Evidence>>>>();
		rightPath = new HashMap<Integer, Map<Integer, List<RightEdge>>>();
		inferredLR = new HashMap<Integer, Set<Integer>>();
//		for (Node start : g.getAllNodes()) {
//			for (Node end : g.getAllNodes()) {
//				int sIndex = start.getIndex();
//				int eIndex = end.getIndex();
//
//				nextHop[sIndex][eIndex] = null;
//			}
//		}
	}

	/**
	 * Add a {@link ReductionEdge} to the graph
	 * 
	 * @param edge
	 *            An edge to be added
	 */
	abstract protected void inferEdge(Node start, Node end, EdgeCondition inferredType, int size, List<Evidence> evidence, boolean isInit);
	
	/**
	 * Return all {@link RightEdge}s from <code>fIndex</code> to
	 * <code>tIndex</code>
	 * 
	 * @param start
	 *            Start node
	 * @param end
	 *            End node
	 * @return All {@link RightEdge}s from <code>start</code> to
	 *         <code>end</code>
	 */
	protected List<RightEdge> getRightEdges(Node start, Node end) {
		if (hasRightEdges(start, end)) {
			return rightPath.get(start.getIndex()).get(end.getIndex());
		} else
			return new ArrayList<RightEdge>();
	}
	
	/**
	 * Return true if there is at least one {@link RightEdge} from
	 * <code>fIndex</code> to <code>tIndex</code>
	 * 
	 * @param start
	 *            Start node
	 * @param end
	 *            End node
	 * @return True if there is at least one {@link RightEdge} from
	 *         <code>start</code> to <code>end</code>
	 */
	protected boolean hasRightEdges(Node start, Node end) {
		if (rightPath.containsKey(start.getIndex()) && rightPath.get(start.getIndex()).containsKey(end.getIndex())) {
			return true;
		} 
		else
			return false;
	}
	
	protected void addNextHop (Node start, Node end, EdgeCondition inferredType, List<Evidence> evidence) {
		if (!nextHop.containsKey(start.getIndex()))
			nextHop.put(start.getIndex(), new HashMap<Integer, Map<EdgeCondition, List<Evidence>>>());
		if (!nextHop.get(start.getIndex()).containsKey(end.getIndex()))
			nextHop.get(start.getIndex()).put(end.getIndex(), new HashMap<EdgeCondition, List<Evidence>>());
		nextHop.get(start.getIndex()).get(end.getIndex()).put(inferredType, evidence);
	}
	
	protected boolean hasNextHop (Node start, Node end, EdgeCondition inferredType) {
		if (nextHop.containsKey(start.getIndex()) && nextHop.get(start.getIndex()).containsKey(end.getIndex())
			&& nextHop.get(start.getIndex()).get(end.getIndex()).containsKey(inferredType))
			return true;
		else
			return false;
	}
	
	protected List<Evidence> getNextHop (Node start, Node end, EdgeCondition inferredType) {
		return nextHop.get(start.getIndex()).get(end.getIndex()).get(inferredType);
	}
	
	/**
	 * Convert all graph edges into {@link ReductionEdge}s
	 */
	protected void initialize() {

		List<Edge> edges = g.getAllEdges();

		for (Edge edge : edges) {
			if (edge instanceof ConstraintEdge || edge instanceof MeetEdge
					|| edge instanceof JoinEdge) {
				if (!edge.getFrom().equals(edge.getTo()))
					inferEdge(edge.getFrom(), edge.getTo(), LeqCondition.getInstance(), 1, new ArrayList<Evidence>(), true);
			} else if (edge instanceof ConstructorEdge) {
				ConstructorEdge e = (ConstructorEdge) edge;
				inferEdge(edge.getFrom(), edge.getTo(), e.getCondition(), 1, new ArrayList<Evidence>(), true);
			}
		}
	}

	/**
	 * Return a path in the constraint graph so that a partial ordering on
	 * <code>start, end</code> can be derived from constraints along the path.
	 * Return null when no such path exits 
	 */
	public List<Edge> getPath(Node start, Node end) {
		List<Edge> path = new ArrayList<Edge>();
		getLeqPath(start, end, LeqCondition.getInstance(), path, false);
		return path;
	}

	/**
	 * Return an LEQ path from <code>start</code> to <code>end</code>
	 * 
	 * @param start
	 *            Start node
	 * @param end
	 *            End node
	 * @return An LEQ path
	 */
	protected void getLeqPath(Node start, Node end, EdgeCondition ec, List<Edge> ret, boolean isRev) {
//		int sIndex = start.getIndex();
//		int eIndex = end.getIndex();
		if (ec instanceof LeqRevCondition) {
//			sIndex = end.getIndex();
//			eIndex = start.getIndex();
			Node tmp = start;
			start = end;
			end = tmp;
			ec = LeqCondition.getInstance();
			isRev = !isRev;
		}
		
		if (ec instanceof LeqCondition && !hasNextHop(start, end, ec))
			return;
//		assert (nextHop[sIndex][eIndex] != null && nextHop[sIndex][eIndex].containsKey(ec));
		
		List<Evidence> evis = getNextHop(start, end, ec);
		// base condition
		if (evis.isEmpty()) {
			Edge current = getOriginalEdge(start, end, ec);
			if (current != null) {
				if (isRev) {
					current = current.getReverse();
				}
				ret.add(current);
			}
		}
		else {
			if (!isRev) {
				// Given a set of evidences evi1 ... evin, we need to construct a path:
				// start --dummyL-- evi1.start ---- evi1.end --dummyR-- evi2.start
				//		 --dummyL--	evi2.start ---- evi2.end --dummyR-- evi3.start
				//       ... 
				//       --dummyL --evin.start ---- evin.end --dummyR-- end
				for (int i=0; i<evis.size(); i++) {
					Evidence evidence = evis.get(i);
					Node next;
					if (i < evis.size() - 1)
						next = evis.get(i+1).start;
					else
						next = end;
					boolean needDummy = !start.equals(evidence.start) || !next.equals(evidence.end);
					if (needDummy)
						ret.add(new DummyEdge(start, evidence.start, true));
					getLeqPath(evidence.start, evidence.end, evidence.ty, ret, isRev);
					if (needDummy)
						ret.add(new DummyEdge(evidence.end, next, false));
					start = evidence.end;
				}
			}
			else {
				for (int i=evis.size()-1; i>=0; i--) {
					Evidence evidence = evis.get(i);
					Node next;
					if (i > 0)
						next = evis.get(i-1).start;
					else
						next = end;
					boolean needDummy = !start.equals(evidence.start) || !next.equals(evidence.end);
					if (needDummy)
						ret.add(new DummyEdge(start, evidence.start, true));
					getLeqPath(evidence.start, evidence.end, evidence.ty, ret, isRev);
					if (needDummy) {
						ret.add(new DummyEdge(evidence.end, next, false));
					}
					start = evidence.end;
				}
			}
		}
	}
	
	/**
	 * Add an atomic LEQ edge to the graph
	 * 
	 * @param startIdx
	 *            Index of start node
	 * @param endIdx
	 *            Index of end node
	 */
	protected void addAtomicLeqEdge (int startIdx, int endIdx) {
		if (!inferredLR.containsKey(startIdx))
			inferredLR.put(startIdx, new HashSet<Integer>());
		inferredLR.get(startIdx).add(endIdx);
	}
	
	/**
	 * Return true if there is an atomic LEQ edge in the graph
	 * 
	 * @param startIdx
	 *            Index of start node
	 * @param endIdx
	 *            Index of end node
	 * @return True if there is an atomic LEQ edge in the graph
	 */
	protected boolean hasAtomicLeqEdge (int startIdx, int endIdx) {
		return inferredLR.containsKey(startIdx) && inferredLR.get(startIdx).contains(endIdx);
	}
	
	/**
	 * Saturate the constraint graph
	 */
	abstract protected void saturation();
	
	protected Edge getOriginalEdge (Node start, Node end, EdgeCondition type) {
		if (g.getEdges(start, end)==null)
			return null;
		for (Edge edge : g.getEdges(start, end)) {
			if (type instanceof LeqCondition
					&& (edge instanceof JoinEdge
							|| edge instanceof MeetEdge || edge instanceof ConstraintEdge)) {
				return edge;
			} else if (edge instanceof ConstructorEdge) {
				ConstructorEdge ce = (ConstructorEdge) edge;
				if (ce.getCondition().equals(type))
					return edge;
			}
		}
		return null;
	}
	
	public static boolean isDashedEdge (Node start) {
		boolean isJoin = start.getElement() instanceof JoinElement;
		return !start.getElement().trivialEnd() && !isJoin;
	}
	
	public static boolean isSolidEdge (Node start) {
		boolean isMeet = start.getElement() instanceof MeetElement;
		return !isDashedEdge(start) || isMeet;
	}
}
