package tool;
import java.io.Serializable;

/**
 * Edge in a node pointing to another node. Contains ID of node it points to, 
 * road type of the edge, distance, max speed and travel time.
 * @author Martin
 *
 */
public class Edge implements Serializable{

	private static final long serialVersionUID = 5272733557733648123L;
	public long nodeID; // Node this edge points to
	public String type; // Type of road see http://wiki.openstreetmap.org/wiki/Key:highway
	public int distance; // Centimeter
	public int maxSpeed = 0; // km/h - 1km/h = 27.7777778cm/second
	public int travelTime = 0; // Using milliseconds
	
	/**
	 * 
	 * @param id of the node this edge points to
	 * @param type of road
	 * @param distance of the road in centimeter
	 */
	public Edge(long id, String type, int distance) {
		nodeID = id;
		this.type = type;
		this.distance = distance;
	}
	
	/**
	 * 
	 * @param id of the node this edge points to
	 * @param type of road
	 * @param distance of the road in centimeter
	 * @param maxSpeed in km/h
	 */
	public Edge(long id, String type, int distance, int maxSpeed) {
		nodeID = id;
		this.type = type;
		this.distance = distance;
		this.maxSpeed = maxSpeed;
	}
	
	/**
	 * 
	 * @param id of the node this edge points to
	 * @param type of road
	 * @param distance of the road in centimeter
	 * @param maxSpeed in km/h
	 * @param travelTime in milliseconds
	 */
	public Edge(long id, String type, int distance, int maxSpeed, int travelTime) {
		nodeID = id;
		this.type = type;
		this.distance = distance;
		this.maxSpeed = maxSpeed;
		this.travelTime = travelTime;
	}
}
