import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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
		//simpleExternalMergeTest();
		
		// Distance between two lat/lon points
		//distanceTest();
		
		// CompleteTest
		completePassTest();

	}
	
	public void simpleXMLTest() {
		
		XMLParser input = new XMLParser();
		try {
			input.parse("testrest.osm", "nodes", "edges", 4096);
		} catch (XMLStreamException | IOException e) {
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
			e.printStackTrace();
		}
		for(int i = 0; i < elements; i++) {
			int val = random.nextInt(1024);
			try {
				oout.writeObject(new IncompleteNode(val,1F,2F));
			} catch (IOException e) {
				e.printStackTrace();
			}
			control[i] = val;
		}
		try {
			oout.flush();
			oout.close();
		} catch (IOException e) {
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(int i = 0; i < elements; i++) {
			IncompleteNode node = null;
			try {
				node = (IncompleteNode) oin.readObject();
			} catch (ClassNotFoundException | IOException e) {
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
			e.printStackTrace();
		}
		
		if(b) {
			System.out.println("Test succeeded");
		}
	}
	
	public void distanceTest() {
		
		GraphProcessor gp = new GraphProcessor(0,0);
		//38.898556		-77.037852
		//38.897147		-77.043934
		int distance = gp.calculateDistance(38.898556, -77.03782, 38.897147, -77.043934);
		System.out.println(distance);
		
	}
	
	public void completePassTest() {
		
		try {
			ArrayList<String> al = new ArrayList<String>();
			al.add("residential");
			al.add("tertiary");
			al.add("unclassified");
			XMLParser parser = new XMLParser();
			parser.parseAndFilterEdges("testrest.osm", "node1", "edge1", 4096,al);
			//parser.parse("testrest.osm", "node1", "edge1", 4096);
			ExternalMergeSort ems = new ExternalMergeSort(200000000,4096,0);
			ems.sortIncompleteNodes("node1", "node2");
			ems.sortIncompleteEdgesByNodeID1("edge1", "edge2");
			GraphProcessor gp = new GraphProcessor(200000000, 4096);
			gp.firstPassCombineIncompleteNodeEdge("node2", "edge2", "edge3");
			ems.sortIncompleteEdgesByNodeID2("edge3", "edge4");
			gp.secondPassCombineIncompleteNodeEdge("node2", "edge4", "edge5");
			ems.sortIncompleteEdgesByNodeID1("edge5", "edge6");
			gp.thirdPassCombineIncompleteNodeEdge("node2", "edge6", "final");
			ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream("final"),4096));
			Node node = (Node) oin.readObject();
			System.out.println("Node id="+node.id);
			for(int i = 0; i < node.edges.size(); i++) {
				Edge edge = node.edges.get(i);
				System.out.println("Edge to id="+edge.nodeID+" of type="+edge.type+" and distance="+edge.distance);
			}
			while(node.edges.size() < 2) {
				node = (Node) oin.readObject();
			}
			System.out.println("Node id="+node.id);
			for(int i = 0; i < node.edges.size(); i++) {
				Edge edge = node.edges.get(i);
				System.out.println("Edge to id="+edge.nodeID+" of type="+edge.type+" and distance="+edge.distance);
			}
			
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}
