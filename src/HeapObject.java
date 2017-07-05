/**
 * Class to store objects to be placed in a heap.
 * @author Martin
 * 
 */
public class HeapObject {

	public long val;
	public int id;
	public Object object;
	
	public HeapObject(long x, int i, Object o) {
		val = x;
		id = i;
		object = o;
	}
}
