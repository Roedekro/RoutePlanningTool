/*
 * Extends IncompleteNode with an address.
 * Does NOT replace IncompleteNode, it is merely
 * used to store the address nodes in a seperate file.
 */

package Address;

import java.io.Serializable;

import tool.IncompleteNode;

public class IncompleteAdrNode extends IncompleteNode {

	private static final long serialVersionUID = 4611381751320472707L;
	public String streetName;
	public int houseNumber;
	
	public IncompleteAdrNode(long id, double lat2, double lon2, String streetName, int houseNumber) {
		super(id,lat2,lon2);
		this.streetName = streetName;
		this.houseNumber = houseNumber;
	}
	
}
