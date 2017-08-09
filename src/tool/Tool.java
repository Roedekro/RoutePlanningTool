package tool;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Tool for reading in Node files.
 * @author Martin
 *
 */
public class Tool {

	private int B = 8192;
	private ObjectInputStream in = null;
	@SuppressWarnings("unused")
	private String file = null;
	private boolean open = false;
	
	/**
	 * Reads in a file of Nodes and returns them as an ArrayList.
	 * Does not require a call to Open() or Close().
	 * @param input File of Nodes
	 * @return ArrayList<Node>
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ArrayList<Node> getNodesAsArrayList(String input) throws FileNotFoundException, IOException {
		
		ArrayList<Node> ret = new ArrayList<Node>();
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input),B));
		Node node = null;
		while(true) {
			try {
				node = (Node) oin.readUnshared();
				ret.add(node);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// EOF
				break;
			}
		}
		oin.close();
	
		return ret;
	}
	
	/**
	 * Reads in a file of Nodes and returns them as a LinkedList.
	 * Does not require a call to Open() or close().
	 * @param input File of Nodes
	 * @return LinkedList<Node>
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public LinkedList<Node> getNodesAsLinkedList(String input) throws FileNotFoundException, IOException {
		
		LinkedList<Node> ret = new LinkedList<Node>();
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input),B));
		Node node = null;
		while(true) {
			try {
				node = (Node) oin.readUnshared();
				ret.add(node);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// EOF
				break;
			}
		}
		oin.close();
	
		return ret;
	}
	
	/**
	 * PRIVATE because getSingleNode doesnt work.
	 * Opens an ObjectInputStream to the given file.
	 * @param file
	 */
	@SuppressWarnings("unused")
	private void open(String file) {
		this.file = file;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file),B));
			open = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * PRIVATE because getSingleNode doesnt work.
	 * Closes the ObjectInputStream.
	 */
	@SuppressWarnings("unused")
	private void close() {
		if(open) {
			try {
				in.close();
				open = false;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * DOESNT WORK. For some reason it will only read the first object repeatedly.
	 * If a file has been opened it will return a Node from the file.
	 * When End Of File is reached it will return null.
	 * If no file is open it will likewhise return null.
	 * @return
	 */
	@SuppressWarnings("unused")
	private Node getSingleNode() {
		Node ret = null;
		if(open) {
			try {
				ret = (Node) in.readUnshared();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				ret = null;
			}
		}
		return ret;
	}		
}
