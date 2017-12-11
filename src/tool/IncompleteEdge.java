package tool;
import java.io.Serializable;

/**
 * Represents an edge that has yet to be added to a node.
 * @author Martin
 *
 */
public class IncompleteEdge implements Serializable {

	private static final long serialVersionUID = 8532488463189916384L;
	protected long nodeID1 = 0;
	protected long nodeID2 = 0;
	protected String type = ""; // Cant be =null for comparisons.
	protected boolean oneway = false;
	protected double lat; // lat coordinate of the first node we look up. This way we can compute the distance
					// between the two nodes when we add the incomplete edge to a node.
	protected double lon; // As above.
	protected int distance; // cm
	protected int maxSpeed = 0; // km/h - 1km/h = 27.7777778cm/second
	protected int travelTime = 0; // Using milliseconds
	
	protected IncompleteEdge(long id1, long id2) {
		nodeID1 = id1;
		nodeID2 = id2;
	}
	
	protected IncompleteEdge(long id1, long id2, String type) {
		nodeID1 = id1;
		nodeID2 = id2;
		this.type = type;
	}
	
	protected IncompleteEdge(long id1, long id2, String type, int distance) {
		nodeID1 = id1;
		nodeID2 = id2;
		this.type = type;
		this.distance = distance;
	}
	
	protected IncompleteEdge(long id1, long id2, String type, int distance, int maxSpeed) {
		nodeID1 = id1;
		nodeID2 = id2;
		this.type = type;
		this.distance = distance;
		this.maxSpeed = maxSpeed;
	}
	
	protected IncompleteEdge(long id1, long id2, String type, int distance, int maxSpeed, int travelTime) {
		nodeID1 = id1;
		nodeID2 = id2;
		this.type = type;
		this.distance = distance;
		this.maxSpeed = maxSpeed;
		this.travelTime = travelTime;
	}
	
	protected IncompleteEdge(long id1, long id2, String type, boolean oneway) {
		nodeID1 = id1;
		nodeID2 = id2;
		this.type = type;
		this.oneway = oneway;
	}
}
