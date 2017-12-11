package tool;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

/**
 * Class responsible for UI and chaining the other classes together.
 * @author Martin
 *
 */
public class Main {
	
	public static long M = 2147483648L; // 2 GigaByte
	public static int B = 8192; // 2 page sizes
	public static int k = (int) (M/B); // 262144
	public static boolean kSet = false;
	public static double minLat = Double.MIN_VALUE;
	public static double maxLat = Double.MAX_VALUE;
	public static double minLon = Double.MIN_VALUE;
	public static double maxLon = Double.MAX_VALUE;
	public static ArrayList<String> filters = new ArrayList<String>();
	public static boolean filterSet = false;

	public static void main(String[] args) {
		
		//test();
		
		System.out.println("=== Route Planning Tool");
		System.out.println("A tool for parsing and processing .osm data from Open Street Map - www.openstreetmap.org. "
				+ "For a list of commands type help.");
		Console console = System.console();
		String in = null;
		
		
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
				System.out.println("=== Alternative <input> <output> === Given a .osm file parses it into a file of Nodes."
						+ " If Box/Filter has been set they will be applied to the Nodes. This method is theoretically faster than Parse +"
						+ "for .osm files roughly matching the bounding box, or when not using a bounding box.");
				System.out.println("=== Print console <input> === Prints a Node file to console. Think twice before using this function!");
				System.out.println("=== Print <input> === Prints a Node file to <input>.txt. This is to manually review small Node files.");
				System.out.println("=== Validate <Node file> <.osm file> === Validates the Node file up against the .osm file. Assumes the .osm "
						+ "file is bounded correctly and will apply the car filters.");
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
					minLat = 54.55;
					maxLat = 57.76;
					minLon = 8;
					maxLon = 12.7;
					String input = split[1];
					String output = split[2];
					parseAndProcess(input,output);
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
			}
			else if(split[0].equalsIgnoreCase("Set")) {
				if(split.length == 2) {
					if(split[1].equalsIgnoreCase("M")) {
						M = Long.parseLong(split[1]);
						if(!kSet) {
							k = (int) (M/B);
						}
					}
					else if(split[1].equalsIgnoreCase("B")) {
						B = Integer.parseInt(split[1]);
						if(!kSet) {
							k = (int) (M/B);
						}
					}
					else if(split[1].equalsIgnoreCase("k")) {
						k = Integer.parseInt(split[1]);
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
					String input = split[1];
					String output = split[2];
					parseAndProcess(input, output);
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
			}
			else if(split[0].equalsIgnoreCase("Print")) {
				if(split[1].equalsIgnoreCase("console") && split.length == 3) {
					ObjectInputStream oin = null;
					try {
						oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(split[2]),B));
						Node node = null;
						while(true) {
							node = (Node) oin.readObject();
							System.out.println("Node id="+node.id+" lat="+node.lat+" lon="+node.lon);
							for(int i = 0; i < node.edges.size(); i++) {
								Edge edge = node.edges.get(i);
								System.out.println("\t Edge id="+edge.nodeID+" type="+edge.type+
										" distance="+edge.distance+" maxSpeed="+edge.maxSpeed);
							}
						}
					} catch (IOException | ClassNotFoundException e) {
						// Done
						try {
							oin.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				else if(split.length == 2) {
					ObjectInputStream oin = null;
					BufferedWriter out = null;
					try {
						oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(split[1]),B));
						out = new BufferedWriter(new FileWriter(split[1]+".txt"));
						Node node = null;
						while(true) {
							node = (Node) oin.readObject();
							out.write("Node id="+node.id+" lat="+node.lat+" lon="+node.lon);
							out.newLine();
							for(int i = 0; i < node.edges.size(); i++) {
								Edge edge = node.edges.get(i);
								out.write("\t Edge id="+edge.nodeID+" type="+edge.type+
										" distance="+edge.distance+" maxSpeed="+edge.maxSpeed+
										" travelTime="+edge.travelTime);
								out.newLine();
							}
						}
					} catch (IOException | ClassNotFoundException e) {
						// Done
						try {
							oin.close();
							out.flush();
							out.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
			}
			else if(split[0].equalsIgnoreCase("Validate")) {
				if(split.length == 3) {
					String nodes = split[1];
					String osm = split[2];
					Validator validator = new Validator(B);
					boolean b = false;
					try {
						b = validator.validate(nodes, osm);
						
					} catch (IOException | XMLStreamException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					finally {
						System.out.println("Validator returned "+b);
					}
					//System.out.println("Validator returned "+b);
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
			}
			else if(split[0].equalsIgnoreCase("alternative")) {
				if(split.length == 3) {
					String input = split[1];
					String output = split[2];
					alternativeParseAndProcess(input, output);
				}
				else {
					System.out.println("Incorrect number of arguments");
				}
			}
			else if(split[0].equalsIgnoreCase("")) {
				// Do nothing
			}
			else {
				System.out.println("Unknown Command");
			}
		}
	}
	
	protected static void parseAndProcess(String input, String output) {
		XMLParser parser = new XMLParser();
		ExternalMergeSort ems = new ExternalMergeSort(M,B,k);
		GraphProcessor gp = new GraphProcessor(M, B);
		long timeStart, timeMid, timeParse, timeProcess, timeTotal;
		try {
			timeStart = System.currentTimeMillis();
			parser.parseBoxAndFilterEdges(input, "node1", "edge1", B, filters, 
					minLat, maxLat, minLon, maxLon);
			timeParse= System.currentTimeMillis() - timeStart;
			System.out.println("Parsing finished in time "+timeParse);
			System.out.println("Input #Nodes = "+parser.numberNodesIn);
			System.out.println("Input #Ways = "+parser.numberWaysIn);
			System.out.println("Output #Nodes = "+parser.numberNodesOut);
			System.out.println("Output #Edges = "+parser.numberEdgesOut);
			timeMid = System.currentTimeMillis();
			System.out.println("Sorting Nodes");
			ems.sortIncompleteNodes("node1", "node2");
			Files.delete(new File("node1").toPath());
			System.out.println("Sorting Edges");
			ems.sortIncompleteEdgesByNodeID1("edge1", "edge2");
			Files.delete(new File("edge1").toPath());
			System.out.println("Scanning 1/3");
			gp.firstPassCombineIncompleteNodeEdge("node2", "edge2", "node3","edge3");
			Files.delete(new File("node2").toPath());
			Files.delete(new File("edge2").toPath());
			System.out.println("Sorting Edges");
			ems.sortIncompleteEdgesByNodeID2("edge3", "edge4");
			Files.delete(new File("edge3").toPath());
			System.out.println("Scanning 2/3");
			gp.secondPassCombineIncompleteNodeEdge("node3", "edge4", "node4", "edge5");
			Files.delete(new File("node3").toPath());
			Files.delete(new File("edge4").toPath());
			System.out.println("Sorting Edges");
			ems.sortIncompleteEdgesByNodeID1("edge5", "edge6");
			Files.delete(new File("edge5").toPath());
			System.out.println("Scanning 3/3");
			gp.thirdPassCombineIncompleteNodeEdge("node4", "edge6", output);
			Files.delete(new File("node4").toPath());
			Files.delete(new File("edge6").toPath());
			timeProcess = System.currentTimeMillis() - timeMid;
			System.out.println("Processing finihed in time "+timeProcess);
			System.out.println("Output #Nodes = "+gp.numberNodesOut);
			System.out.println("Output #Edges = "+gp.numberEdgesOut);
			timeTotal = timeParse + timeProcess;
			System.out.println("Total time was "+timeTotal);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static void alternativeParseAndProcess(String input, String output) {
		XMLParser parser = new XMLParser();
		ExternalMergeSort ems = new ExternalMergeSort(M,B,k);
		GraphProcessor gp = new GraphProcessor(M, B);
		long timeStart, timeMid, timeParse, timeProcess, timeTotal;
		try {
			timeStart = System.currentTimeMillis();
			parser.alternativeParse(input, "node1", "edge1", B, filters, 
					minLat, maxLat, minLon, maxLon);
			timeParse= System.currentTimeMillis() - timeStart;
			System.out.println("Parsing finished in time "+timeParse);
			System.out.println("Input #Nodes = "+parser.numberNodesIn);
			System.out.println("Input #Ways = "+parser.numberWaysIn);
			System.out.println("Output #Nodes = "+parser.numberNodesOut);
			System.out.println("Output #Edges = "+parser.numberEdgesOut);
			timeMid = System.currentTimeMillis();
			ems.sortIncompleteNodes("node1", "node2");
			Files.delete(new File("node1").toPath());
			ems.sortIncompleteEdgesByNodeID2("edge1", "edge2");
			Files.delete(new File("edge1").toPath());
			gp.alternativeFirstPass("node2", "edge2", "node3","edge3");
			Files.delete(new File("node2").toPath());
			Files.delete(new File("edge2").toPath());
			ems.sortIncompleteEdgesByNodeID1("edge3", "edge4");
			Files.delete(new File("edge3").toPath());
			gp.alternativeSecondPass("node3", "edge4", output);
			Files.delete(new File("node3").toPath());
			Files.delete(new File("edge4").toPath());
			timeProcess = System.currentTimeMillis() - timeMid;
			System.out.println("Processing finihed in time "+timeProcess);
			System.out.println("Output #Nodes = "+gp.numberNodesOut);
			System.out.println("Output #Edges = "+gp.numberEdgesOut);
			timeTotal = timeParse + timeProcess;
			System.out.println("Total time was "+timeTotal);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static void parseAndProcessWithAddresses(String input, String output) {
		XMLParser parser = new XMLParser();
		ExternalMergeSort ems = new ExternalMergeSort(M,B,k);
		GraphProcessor gp = new GraphProcessor(M, B);
		long timeStart, timeMid, timeParse, timeProcess, timeTotal;
		try {
			timeStart = System.currentTimeMillis();
			// New parse spits out a file of IncompleteAdrNodes
			parser.parseBoxAndFilterEdgesWithAddresses(input, "node1", "edge1", "adr1", B, filters, 
					minLat, maxLat, minLon, maxLon);
			timeParse= System.currentTimeMillis() - timeStart;
			System.out.println("Parsing finished in time "+timeParse);
			System.out.println("Input #Nodes = "+parser.numberNodesIn);
			System.out.println("Input #Ways = "+parser.numberWaysIn);
			System.out.println("Output #Nodes = "+parser.numberNodesOut);
			System.out.println("Output #Edges = "+parser.numberEdgesOut);
			// Nearly normal buildup of the road network.
			timeMid = System.currentTimeMillis();
			ems.sortIncompleteNodes("node1", "node2");
			Files.delete(new File("node1").toPath());
			ems.sortIncompleteEdgesByNodeID2("edge1", "edge2");
			Files.delete(new File("edge1").toPath());
			gp.alternativeFirstPass("node2", "edge2", "node3","edge3");
			Files.delete(new File("node2").toPath());
			Files.delete(new File("edge2").toPath());
			ems.sortIncompleteEdgesByNodeID1("edge3", "edge4");
			Files.delete(new File("edge3").toPath());
			gp.alternativeSecondPass("node3", "edge4", output);
			Files.delete(new File("node3").toPath());
			Files.delete(new File("edge4").toPath());
			timeProcess = System.currentTimeMillis() - timeMid;
			System.out.println("Processing finihed in time "+timeProcess);
			System.out.println("Output #Nodes = "+gp.numberNodesOut);
			System.out.println("Output #Edges = "+gp.numberEdgesOut);
			timeTotal = timeParse + timeProcess;
			System.out.println("Total time was "+timeTotal);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static void test() {
		
		Tool tool = new Tool();
		try {
			tool.getNodesAsArrayList("Roedekro");
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		Test test = new Test();
		//test.run();	
		
	}
}
