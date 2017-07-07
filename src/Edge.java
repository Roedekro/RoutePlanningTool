import java.io.Serializable;

/**
 * Edge in a node pointing to another node.
 * @author Martin
 *
 */
public class Edge implements Serializable{

	private static final long serialVersionUID = 5272733557733648123L;
	public long nodeID;
	public String type;
	public int distance; // Centimeter
	int maxSpeed = 0;
	
	/**
	 * 
	 * @param id of the node this edge points to
	 * @param type of road
	 * @param distance of the road
	 */
	public Edge(long id, String type, int distance) {
		nodeID = id;
		this.type = type;
		this.distance = distance;
	}
	
	public Edge(long id, String type, int distance, int maxSpeed) {
		nodeID = id;
		this.type = type;
		this.distance = distance;
		this.maxSpeed = maxSpeed;
	}
}
