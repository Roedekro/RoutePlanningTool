import java.io.Serializable;

/**
 * Represents an edge that has yet to be added to a node.
 * @author Martin
 *
 */
public class IncompleteEdge implements Serializable {

	private static final long serialVersionUID = 8532488463189916384L;
	public long nodeID1;
	public long nodeID2;
	public String type;
	public boolean oneway = false;
	public float lat; // lat coordinate of the first node we look up. This way we can compute the distance
					// between the two nodes when we add the incomplete edge to a node.
	public float lon; // As above.
	
	public IncompleteEdge(long id1, long id2) {
		nodeID1 = id1;
		nodeID2 = id2;
	}
	
	public IncompleteEdge(long id1, long id2, String type) {
		nodeID1 = id1;
		nodeID2 = id2;
		this.type = type;
	}
	
	public IncompleteEdge(long id1, long id2, String type, boolean oneway) {
		nodeID1 = id1;
		nodeID2 = id2;
		this.type = type;
		this.oneway = oneway;
	}
}
