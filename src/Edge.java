/**
 * Edge in a node pointing to another node.
 * @author Martin
 *
 */
public class Edge {

	public long nodeID;
	public String type;
	public int distance;
	
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
}
