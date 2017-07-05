import java.util.ArrayList;

/**
 * Node in the graph, containing edges to other nodes.
 * @author Martin
 *
 */
public class Node {

	public long id;
	public float lat;
	public float lon;
	public ArrayList<Edge> edges;
	
	public Node(long id, float lat, float lon) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		edges = new ArrayList<Edge>();
	}
	
	public void addEdge(Edge e) {
		edges.add(e);
	}
	
	public ArrayList<Edge> getEdges() {
		return edges;
	}
}
