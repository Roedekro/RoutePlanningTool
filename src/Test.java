import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

/**
 * Class responsible for testing the other classes.
 * Made to unclutter Main.
 * @author Martin
 *
 */
public class Test {

	public void run() {
		// XML parsing
		//simpleXMLTest();
		
		// External Sorting
		simpleExternalMergeTest();

	}
	
	public void simpleXMLTest() {
		
		XMLParser input = new XMLParser();
		try {
			input.parse("testrest.osm", "nodes", "edges", 4096);
		} catch (XMLStreamException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void simpleExternalMergeTest() {
		
		int M = 96; // 4 elements
		int B = 48; // 2 elements
		int elements = 128;
		Random random = new Random();
		int[] control = new int[elements];
		
		// Write IncompleteNodes to disk
		ObjectOutputStream oout = null;
		try {
			oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("input"),B));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i = 0; i < elements; i++) {
			int val = random.nextInt(1024);
			try {
				oout.writeObject(new IncompleteNode(val,1F,2F));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			control[i] = val;
		}
		try {
			oout.flush();
			oout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Arrays.sort(control);
		
		ExternalMergeSort ex = new ExternalMergeSort(M, B, 0);
		ex.sortIncompleteNodes("input", "output");
		
		boolean b = true;
		ObjectInputStream oin = null;
		try {
			oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream("output"),B));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i = 0; i < elements; i++) {
			IncompleteNode node = null;
			try {
				node = (IncompleteNode) oin.readObject();
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println(node.id);
			if(node.id != control[i]) {
				System.out.println("MISMATCH!!! "+node.id+" "+control[i]);
				b = false;
			}
		}
		try {
			oin.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(b) {
			System.out.println("Test succeeded");
		}
	}
}
