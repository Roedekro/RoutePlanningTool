package tool;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import elements.Edge;
import elements.Node;

/**
 * Class responsible for testing the other classes.
 * Made to unclutter Main.
 * @author Martin
 *
 */
class Test {

	protected void run() {
		
		// Rundkørsel test
		testRundkoersel();		
		
		// XML parsing
		//simpleXMLTest();
		
		// External Sorting
		//simpleExternalMergeTest();
		
		// Distance between two lat/lon points
		//distanceTest();
		
		// CompleteTest
		//completePassTest();

	}
	
	protected void testRundkoersel() {
		
		long M = 2147483648L; // 2 GigaByte
		int B = 8192; // 2 page sizes
		int k = (int) (M/B); // 262144
		ArrayList<String> filters = new ArrayList<String>();
		filters.add("motorway");
		filters.add("trunk");
		filters.add("primary");
		filters.add("secondary");
		filters.add("tertiary");
		filters.add("unclassified");
		filters.add("residential");
		filters.add("service");
		filters.add("motorway_link");
		filters.add("trunk_link");
		filters.add("primary_link");
		filters.add("secondary_link");
		filters.add("tertiary_link");
		filters.add("living_street");
		filters.add("road");
		double minLat = 54.55;
		double maxLat = 57.76;
		double minLon = 8;
		double maxLon = 12.7;
		String input = "rundkoersel.osm";
		String output = "rundNoder";
		XMLParser parser = new XMLParser();
		ExternalMergeSort ems = new ExternalMergeSort(M,B,k);
		GraphProcessor gp = new GraphProcessor(M, B);
		try {
			parser.parseBoxAndFilterEdges(input, "node1", "edge1", B, filters, 
					minLat, maxLat, minLon, maxLon);
			ems.sortIncompleteNodes("node1", "node2");
			Files.delete(new File("node1").toPath());
			ems.sortIncompleteEdgesByNodeID1("edge1", "edge2");
			Files.delete(new File("edge1").toPath());
			gp.firstPassCombineIncompleteNodeEdge("node2", "edge2", "node3","edge3");
			Files.delete(new File("node2").toPath());
			Files.delete(new File("edge2").toPath());
			ems.sortIncompleteEdgesByNodeID2("edge3", "edge4");
			Files.delete(new File("edge3").toPath());
			gp.secondPassCombineIncompleteNodeEdge("node3", "edge4", "node4", "edge5");
			Files.delete(new File("node3").toPath());
			Files.delete(new File("edge4").toPath());
			ems.sortIncompleteEdgesByNodeID1("edge5", "edge6");
			Files.delete(new File("edge5").toPath());
			gp.thirdPassCombineIncompleteNodeEdge("node4", "edge6", output);
			Files.delete(new File("node4").toPath());
			Files.delete(new File("edge6").toPath());
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void simpleXMLTest() {
		
		XMLParser input = new XMLParser();
		try {
			input.parse("testrest.osm", "nodes", "edges", 4096);
		} catch (XMLStreamException | IOException e) {
			e.printStackTrace();
		}
	}
	
	
	protected void simpleExternalMergeTest() {
		
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
	
	protected void distanceTest() {
		
		GraphProcessor gp = new GraphProcessor(0,0);
		//38.898556		-77.037852
		//38.897147		-77.043934
		int distance = gp.calculateDistance(38.898556, -77.03782, 38.897147, -77.043934);
		System.out.println(distance);
		
	}
	
	protected void completePassTest() {
		
		try {
			ArrayList<String> al = new ArrayList<String>();
			al.add("residential");
			al.add("tertiary");
			al.add("unclassified");
			XMLParser parser = new XMLParser();
			parser.parseAndFilterEdges("testrest.osm", "node1", "edge1", 4096,al);
			//parser.parse("testrest.osm", "node1", "edge1", 4096);
			ExternalMergeSort ems = new ExternalMergeSort(200000000,4096,0);
			GraphProcessor gp = new GraphProcessor(200000000, 4096);		
			ems.sortIncompleteNodes("node1", "node2");
			Files.delete(new File("node1").toPath());
			ems.sortIncompleteEdgesByNodeID1("edge1", "edge2");
			Files.delete(new File("edge1").toPath());
			gp.firstPassCombineIncompleteNodeEdge("node2", "edge2", "node3","edge3");
			Files.delete(new File("node2").toPath());
			Files.delete(new File("edge2").toPath());
			ems.sortIncompleteEdgesByNodeID2("edge3", "edge4");
			Files.delete(new File("edge3").toPath());
			gp.secondPassCombineIncompleteNodeEdge("node3", "edge4", "node4", "edge5");
			Files.delete(new File("node3").toPath());
			Files.delete(new File("edge4").toPath());
			ems.sortIncompleteEdgesByNodeID1("edge5", "edge6");
			Files.delete(new File("edge5").toPath());
			gp.thirdPassCombineIncompleteNodeEdge("node4", "edge6", "final");
			Files.delete(new File("node4").toPath());
			Files.delete(new File("edge6").toPath());
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
