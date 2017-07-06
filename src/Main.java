import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

/**
 * Class responsible for UI and chaining the other classes together.
 * @author Martin
 *
 */
public class Main {

	public static void main(String[] args) {
		
		//test();
		
		System.out.println("=== Route Planning Tool");
		System.out.println("A tool for parsing and processing .osm data from Open Street Map - www.openstreetmap.org");
		System.out.println("For a list of commands type help");
		Console console = System.console();
		String in = null;
		long M = 2147483648L; // 2 GigaByte
		int B = 8192; // 2 page sizes
		int k = (int) (M/B); // 262144 M/B
		boolean kSet = false;
		double minLat = Double.MAX_VALUE;
		double maxLat = Double.MAX_VALUE;
		double minLon = Double.MAX_VALUE;
		double maxLon = Double.MAX_VALUE;
		ArrayList<String> filters = new ArrayList<String>();
		boolean filterSet = false;
		
		while(true) {
			in = console.readLine();
			String[] split = in.split(" ");
			if(split[0].equalsIgnoreCase("help")) {
				System.out.println("=== Commands are:");
				System.out.println("=== Danmark <input> <output> === Given a .osm file it will process it and "
						+ "output a file of Nodes boxed to the State of Denmark, containing all car accessible roads.");
				System.out.println("=== Parameters === Lists the current parameters.");
				System.out.println("=== Set <parameter> <integer> === M for memory, B for block size, and k for k-way merge. "
						+ "Be advised that k by default is M/B, but that your OS will likely not grant you that many file handles.");
				System.out.println("=== Box <min Latitude> <max Latitude> <min Longitude> <max Longitude> === "
						+ "Boxes the data to the given parameters. Uses the standard format of example 12.3456 or -12.345678.");
				System.out.println("=== Filter car === Filters roads to only include roads accessible by cars.");
				System.out.println("=== Filter <String> <String> ... <String> === Filters all edges to the "
						+ "specified type of roads. See http://wiki.openstreetmap.org/wiki/Key:highway for filters.");
				System.out.println("=== Parse <input> <output> === Given a .osm file parses it into a file of Nodes."
						+ " If Box/Filter has been set they will be applied to the Nodes.");
				System.out.println("=== Exit");
				/*System.out.println("======");
				System.out.println("Tool written by Martin Jacobsen");
				System.out.println("======");*/
			}
			else if(split[0].equalsIgnoreCase("exit")) {
				System.exit(0);
			}
			else if(split[0].equalsIgnoreCase("Danmark")) {
				if(split.length == 3) {
					if(filterSet) {
						filters = new ArrayList<String>();
					}
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
					minLat = 54;
					maxLat = 58;
					minLon = 8;
					maxLon = 13;
					String input = split[2];
					String output = split[3];
					XMLParser parser = new XMLParser();
					ExternalMergeSort ems = new ExternalMergeSort(M,B,k);
					GraphProcessor gp = new GraphProcessor(M, B);
					try {
						parser.parseBoxAndFilterEdges(input, "node1", "edge1", B, filters, 
								minLat, maxLat, minLon, maxLon);
						ems.sortIncompleteNodes("node1", "node2");
						ems.sortIncompleteEdgesByNodeID1("edge1", "edge2");
						gp.firstPassCombineIncompleteNodeEdge("node2", "edge2", "edge3");
						ems.sortIncompleteEdgesByNodeID2("edge3", "edge4");
						gp.secondPassCombineIncompleteNodeEdge("node2", "edge4", "edge5");
						ems.sortIncompleteEdgesByNodeID1("edge5", "edge6");
						gp.thirdPassCombineIncompleteNodeEdge("node2", "edge6", output);
					} catch (XMLStreamException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
				
				
				if(filterSet) {
					filters = new ArrayList<String>();
				}
				filterSet = true;

			}
			else if(split[0].equalsIgnoreCase("Set")) {
				if(split.length == 2) {
					if(split[1].equalsIgnoreCase("M")) {
						M = Long.parseLong(split[2]);
						if(!kSet) {
							k = (int) (M/B);
						}
					}
					else if(split[1].equalsIgnoreCase("B")) {
						B = Integer.parseInt(split[2]);
						if(!kSet) {
							k = (int) (M/B);
						}
					}
					else if(split[1].equalsIgnoreCase("k")) {
						k = Integer.parseInt(split[2]);
						kSet = true;
					}
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
				
			}
			else if(split[0].equalsIgnoreCase("Parameters")) {
				System.out.println("M = "+M);
				System.out.println("B = "+B);
				System.out.println("k = "+k);
			}
			else if(split[0].equalsIgnoreCase("Box")) {
				if(split.length == 5) {
					minLat = Double.parseDouble(split[1]);
					maxLat = Double.parseDouble(split[2]);
					minLon = Double.parseDouble(split[3]);
					maxLon = Double.parseDouble(split[4]);
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
			}
			else if(split[0].equalsIgnoreCase("Filter")) {
				if(split.length < 2) {
					System.out.println("Please provide some arguments");
				}
				else if(split[1].equalsIgnoreCase("car")) {
					if(filterSet) {
						filters = new ArrayList<String>();
					}
					filterSet = true;
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
				}
				else {
					if(filterSet) {
						filters = new ArrayList<String>();
					}
					filterSet = true;
					for(int i = 1; i < split.length; i++) {
						filters.add(split[i]);
					}				
				}
			}
			else if(split[0].equalsIgnoreCase("Parse")) {
				if(split.length == 3) {
					String input = split[2];
					String output = split[3];
					XMLParser parser = new XMLParser();
					ExternalMergeSort ems = new ExternalMergeSort(M,B,k);
					GraphProcessor gp = new GraphProcessor(M, B);
					try {
						if(filterSet && minLat != Double.MAX_VALUE) {
							parser.parseBoxAndFilterEdges(input, "node1", "edge1", B, filters, 
									minLat, maxLat, minLon, maxLon);
						}
						else if(filterSet) {
							parser.parseAndFilterEdges(input, "node1", "edge1", B, filters);
						}
						else if(minLat != Double.MAX_VALUE) {
							parser.parseAndBox(input, "node1", "edge1", B, minLat, maxLat, minLon, maxLon);
						}
						else {
							parser.parse(input, "node1", "edge1", B);
						}
						ems.sortIncompleteNodes("node1", "node2");
						ems.sortIncompleteEdgesByNodeID1("edge1", "edge2");
						gp.firstPassCombineIncompleteNodeEdge("node2", "edge2", "edge3");
						ems.sortIncompleteEdgesByNodeID2("edge3", "edge4");
						gp.secondPassCombineIncompleteNodeEdge("node2", "edge4", "edge5");
						ems.sortIncompleteEdgesByNodeID1("edge5", "edge6");
						gp.thirdPassCombineIncompleteNodeEdge("node2", "edge6", output);
					} catch (XMLStreamException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
			}
		}
	}
	
	public static void test() {
		
		Test test = new Test();
		test.run();	
		
	}
}
