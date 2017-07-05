import java.io.Serializable;

/**
 * Represents a node without any edges.
 * @author Martin
 *
 */
public class IncompleteNode implements Serializable{

	private static final long serialVersionUID = -4896887724692721954L;
	public long id;
	public float lat;
	public float lon;
	
	public IncompleteNode(long id, float lat, float lon) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
	}
}
