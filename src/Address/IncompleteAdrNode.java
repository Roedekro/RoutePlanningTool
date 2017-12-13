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
	public String street;
	public String house;
	
	public IncompleteAdrNode(long id, double lat2, double lon2, String street, String house) {
		super(id,lat2,lon2);
		this.street = street;
		this.house = house;
	}
	
}
