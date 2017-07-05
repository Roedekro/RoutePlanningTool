/**
 * MinHeap for use in external merge sort.
 * Based on Heapsort by J. W. J. Williams' "Algorithm 232" published in
 * Publications of the ACM, 7 (1964).
 * NOTE: The array must be 1-indexed!
 * @author Martin
 * 
 */
public class Heap {

	/**
	 * Works by expanding the array by one, then bubbling the input
	 * element up the height of the heap, finding a place where
	 * it maintains the heap order. O(log n).
	 * @param array Array of HeapObjects maintaining heap order
	 * @param in HeapObject to be placed in array
	 * @param n Number of elements already placed in the array
	 */
	public void inheap(HeapObject[] array, HeapObject in, int n) {
		
		n++;
		int i = n;
		int j;
		boolean b = true;
		while(b) {
			if(i > 1) {
				j = i/2;
				if(in.val < array[j].val) {
					array[i] = array[j];
					i = j;
				}
				else {
					b = false;
				}
			}
			else {
				b = false;
			}
			array[i] = in;
		}
	}
	
	/**
	 * Places the in element in the heap, and returns the minimum element.
	 * It does so by removing the smallest element and replacing it with in,
	 * and then bubbles down in until heap order is maintained. O(log n).
	 * @param array Array of HeapObjects maintaining heap order
	 * @param in HeapObject to be placed in array
	 * @param n Number of elements already placed in the array
	 */
	public HeapObject SWOPHeap(HeapObject[] array, HeapObject in, int n) {
		
		HeapObject out = null;
		if(n < 1) {
			out = in;
		}
		else if(in.val <= array[1].val) {
			out = in;
		}
		else {
			int i = 1;
			array[n+1] = in;
			out = array[1];
			boolean b = true;
			int j;
			HeapObject temp;
			HeapObject temp1;
			while(b) {
				j = i+i;
				if(j <= n) {
					temp = array[j];
					temp1 = array[j+1];
					if(temp1.val < temp.val) {
						temp = temp1;
						j++;
					}
					if(temp.val < in.val) {
						array[i] = temp;
						i = j;
					}
					else {
						b = false;
					}
				}
				else {
					b = false;
				}
				array[i] = in;
			}
		}
		return out;
	}
	
	/**
	 * Removes and returns the minimum element of the array.
	 * @param array Array of HeapObjects maintaining heap order
	 * @param n Number of elements in the array
	 * @return
	 */
	public HeapObject outheap(HeapObject[] array, int n) {
		HeapObject in = array[n];
		n--;
		return SWOPHeap(array,in,n);
	}
	
	/**
	 * Restores an array of HeapObjects to heap order. O(n log n).
	 * @param array Array of HeapObjects NOT maintaining heap order
	 * @param n Number of elements in the array
	 */
	public void setheap(HeapObject[] array, int n) {
		int j = 1;
		boolean b = true;
		while(b) {
			inheap(array,array[j+1],j);
			j++;
			if(j >= n) {
				b = false;
			}
		}
	}
	

}
