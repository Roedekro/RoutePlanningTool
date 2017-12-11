package Address;

import java.io.Serializable;

public class AngleEdge implements Serializable {

	private static final long serialVersionUID = -6513603502229893135L;
	public long nodeid; // Of destination
	public int weight; // Length of the edge
	public String type;
	public String street;
	public int angle; // Angle relative to north, also known as bearing.
	
	public AngleEdge(long nodeid, int weight, String type, int angle, String street) {
		this.nodeid = nodeid;
		this.weight = weight;
		this.type = type;
		this.angle = angle;
		this.street = street;
	}
}
