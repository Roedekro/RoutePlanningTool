package elements;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Node in the graph, containing an id, latitude, longitude, and edges to other nodes.
 * @author Martin
 *
 */
public class Node implements Serializable {

	private static final long serialVersionUID = -3416046260638920518L;
	public long id; // Node id from openstreetmap
	public double lat; // Latitude
	public double lon; // Longitude
	public ArrayList<Edge> edges; // Array of edges that points to other nodes
	
	public Node(long id, double lat2, double lon2) {
		this.id = id;
		this.lat = lat2;
		this.lon = lon2;
		edges = new ArrayList<Edge>();
	}
	
	public void addEdge(Edge e) {
		edges.add(e);
	}
	
	public ArrayList<Edge> getEdges() {
		return edges;
	}
}
