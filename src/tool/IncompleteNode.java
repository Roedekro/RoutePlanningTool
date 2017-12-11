package tool;
import java.io.Serializable;

/**
 * Represents a node without any edges.
 * @author Martin
 *
 */
public class IncompleteNode implements Serializable{

	private static final long serialVersionUID = -4896887724692721954L;
	protected long id;
	protected double lat;
	protected double lon;
	protected boolean pointedFromTo = false;
	
	public IncompleteNode(long id, double lat2, double lon2) {
		this.id = id;
		this.lat = lat2;
		this.lon = lon2;
	}
}
