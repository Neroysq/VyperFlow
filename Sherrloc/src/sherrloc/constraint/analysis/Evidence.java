package sherrloc.constraint.analysis;

import sherrloc.graph.EdgeCondition;
import sherrloc.graph.Node;

public class Evidence {
	public Node start, end;
	public EdgeCondition ty;
	
	public Evidence(Node start, Node end, EdgeCondition type) {
		this.start = start;
		this.end = end;
		this.ty = type;
	}
}
