/*
 * Node containing an address and AngleEdges as opposed to normal Edges.
 */

package Address;

import java.io.Serializable;
import java.util.ArrayList;

public class AdrNode implements Serializable {
	
	private static final long serialVersionUID = 7064378951613060561L;
	public long id; // Node id from openstreetmap
	public double lat; // Latitude
	public double lon; // Longitude
	public String street = ""; // Street Name, if relevant
	public String house; // Median of house numbers closest to this point, if relevant.
	public ArrayList<AngleEdge> edges; // Array of edges with angles that points to other nodes
	
	// Working space
	public ArrayList<AngleEdge> reverseEdges;
	public ArrayList<String> houseNumbers = null;
	
	public AdrNode(long id, double lat, double lon) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		edges = new ArrayList<AngleEdge>();
	}
	
	public void addEdge(AngleEdge edge) {
		edges.add(edge);
	}
	
	public void addReverseEdge(AngleEdge edge) {
		reverseEdges.add(edge);
	}
	
	public void addHousenumber(String number) {
		houseNumbers.add(number);
	}

}
