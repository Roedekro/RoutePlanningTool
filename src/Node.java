import java.io.Serializable;
import java.util.ArrayList;

/**
 * Node in the graph, containing edges to other nodes.
 * @author Martin
 *
 */
public class Node implements Serializable {

	private static final long serialVersionUID = -3416046260638920518L;
	public long id;
	public double lat;
	public double lon;
	public ArrayList<Edge> edges;
	
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
