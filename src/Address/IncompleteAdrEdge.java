/*
 * Extends IncompleteEde with a a street name and an angle
 */
package Address;

import tool.IncompleteEdge;

public class IncompleteAdrEdge extends IncompleteEdge {
	
	private static final long serialVersionUID = -5487564991817488948L;
	public String streetName;
	public int angle;
	
	public IncompleteAdrEdge(long id1, long id2) {
		super(id1,id2);
	}
	
	public IncompleteAdrEdge(long id1, long id2, String type, String streetName) {
		super(id1,id2,type);
		this.streetName = streetName;
	}

	public IncompleteAdrEdge(long id1, long id2, String type, int distance, int maxSpeed, int travelTime, String streetName, int angle) {

		super(id1,id2,type,distance,maxSpeed,travelTime);
		this.streetName = streetName;
		this.angle = angle;
	}
	
	public IncompleteAdrEdge(long id1, long id2, String type, boolean oneway, String streetName) {
		super(id1,id2,type,oneway);
		this.streetName = streetName;
	}

}
