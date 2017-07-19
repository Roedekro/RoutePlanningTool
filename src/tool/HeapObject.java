package tool;
/**
 * Class to store objects to be placed in a heap.
 * @author Martin
 * 
 */
class HeapObject {

	protected long val;
	protected int id;
	protected Object object;
	
	protected HeapObject(long x, int i, Object o) {
		val = x;
		id = i;
		object = o;
	}
}
