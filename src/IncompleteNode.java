import java.io.Serializable;

/**
 * Represents a node without any edges.
 * @author Martin
 *
 */
public class IncompleteNode implements Serializable{

	private static final long serialVersionUID = -4896887724692721954L;
	public long id;
	public double lat;
	public double lon;
	
	public IncompleteNode(long id, double lat2, double lon2) {
		this.id = id;
		this.lat = lat2;
		this.lon = lon2;
	}
}
