import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class is responsible for managing the various stages of processing of our graph.
 * @author Martin
 *
 */
public class GraphProcessor {
	
	long M = 2000000000;
	int B = 4096;
	
	public GraphProcessor(long M, int B) {
		if(M > 0) this.M = M;
		if(B > 0) this.B = B;
	}
	
	/**
	 * First pass that compares nodeID1 of edges with the nodes,
	 * removing any edge that doesnt have a corresponding node.
	 * Also populates the lat and lon of the edges.
	 * @param nodes File containing sorted IncompleteNodes
	 * @param edges File containing IncompleteEdges sorted by nodeID1
	 * @param output File that will contain the filtered sorted list of IncompleteEdges
	 */
	@SuppressWarnings("resource")
	public void firstPassCombineIncompleteNodeEdge(String nodes, String edges, String output) {
		
		ObjectInputStream inNodes = null;
		ObjectInputStream inEdges = null;
		ObjectOutputStream out = null;
		
		try {
			inNodes = new ObjectInputStream(new BufferedInputStream(new FileInputStream(nodes),B));
			inEdges = new ObjectInputStream(new BufferedInputStream(new FileInputStream(edges),B));
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(output),B));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		IncompleteNode node = null;
		IncompleteEdge edge = null;
		try {
			node = (IncompleteNode) inNodes.readObject();
			edge = (IncompleteEdge) inEdges.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		
		// Loop through the two files until one runs out
		while(true) {
			
			if(edge.nodeID1 == node.id) {
				edge.lat = node.lat;
				edge.lon = node.lon;
				try {
					out.writeObject(edge);
					edge = (IncompleteEdge) inEdges.readObject();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					break;
				}
			}
			else if(edge.nodeID1 < node.id) {
				try {
					edge = (IncompleteEdge) inEdges.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					break;
				}
			}
			else {
				try {
					node = (IncompleteNode) inNodes.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of nodes, break
					break;
				}
			}
		}
		
		// Clean up
		try {
			out.flush();
			out.close();
			inNodes.close();
			inEdges.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Second pass that compares nodeID2 of edges with the nodes,
	 * removing any edge that doesnt have a corresponding node.
	 * Copies each valid edge, with the copy having switched 
	 * nodeID1 and nodeID2. There is no copy of oneway edges.
	 * @param nodes File containing sorted IncompleteNodes
	 * @param edges File containing IncompleteEdges sorted by nodeID2
	 * @param output File that will contain the finished (NOT sorted) list of IncompleteEdges
	 */
	public void secondPassCombineIncompleteNodeEdge(String nodes, String edges, String output) {
		
		ObjectInputStream inNodes = null;
		ObjectInputStream inEdges = null;
		ObjectOutputStream out = null;
		
		try {
			inNodes = new ObjectInputStream(new BufferedInputStream(new FileInputStream(nodes),B));
			inEdges = new ObjectInputStream(new BufferedInputStream(new FileInputStream(edges),B));
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(output),B));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		IncompleteNode node = null;
		IncompleteEdge edge = null;
		try {
			node = (IncompleteNode) inNodes.readObject();
			edge = (IncompleteEdge) inEdges.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		
		// Loop through the two files until one runs out
		while(true) {
			
			if(edge.nodeID2 == node.id) {
				
				// Calculate the distance
				int distance = calculateDistance(node.lat, node.lon, edge.lat, edge.lon);
				edge.distance = distance;
				
				// If not oneway add a copy with switched nodeID1 and nodeID2
				if(!edge.oneway) {
					try {
						out.writeObject(new IncompleteEdge(edge.nodeID2,edge.nodeID1,edge.type,distance));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				try {
					out.writeObject(edge);
					edge = (IncompleteEdge) inEdges.readObject();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					break;
				}
			}
			else if(edge.nodeID2 < node.id) {
				try {
					edge = (IncompleteEdge) inEdges.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					break;
				}
			}
			else {
				try {
					node = (IncompleteNode) inNodes.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of nodes, break
					break;
				}
			}
		}
		
		// Clean up
		try {
			out.flush();
			out.close();
			inNodes.close();
			inEdges.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Calculates the distance between two points denoted by lat/lon
	 * according to the haversine forumula. See
	 * https://en.wikipedia.org/wiki/Haversine_formula
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return distance in meters
	 */
	public int calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		
		double piDiv180 = Math.PI / 180;
		double lat1r = lat1 * piDiv180;
		//double lon1r = lon1 * piDiv180;
		double lat2r = lat2 * piDiv180;
		//double lon2r = lon2 * piDiv180;
		double deltaLatr = (lat2-lat1) * piDiv180;
		double deltaLonr = (lon2-lon1) * piDiv180;
		
		double expression1 = Math.sin(deltaLatr/2) * Math.sin(deltaLatr/2);
		double expression2 = Math.sin(deltaLonr/2) * Math.sin(deltaLonr/2);
		double expression3 = Math.cos(lat1r) * Math.cos(lat2r) * expression2;
		double expression4 = Math.sqrt(expression1 + expression3);
		// 2x the earths radius times arcsin of the above
		double distance = 12742 * Math.asin(expression4);
		// Different way of writing the above
		//double distance = 12742 * Math.atan2(expression4, Math.sqrt(1-expression1-expression3));
		
		return (int) Math.round(distance*1000);
		
	}
	
	/**
	 * Third pass that compares adds creates Nodes and adds IncompleteEdges
	 * to these nodes. Removes nodes with no edges.
	 * @param nodes File containing sorted IncompleteNodes
	 * @param edges File containing IncompleteEdges sorted by nodeID1
	 * @param output File that will contain the finished sorted list of Nodes with Edges
	 */
	public void thirdPassCombineIncompleteNodeEdge(String nodes, String edges, String output) {
		
		ObjectInputStream inNodes = null;
		ObjectInputStream inEdges = null;
		ObjectOutputStream out = null;
		
		try {
			inNodes = new ObjectInputStream(new BufferedInputStream(new FileInputStream(nodes),B));
			inEdges = new ObjectInputStream(new BufferedInputStream(new FileInputStream(edges),B));
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(output),B));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		IncompleteNode inNode = null;
		IncompleteEdge inEdge = null;
		Node node = null;
		Edge edge = null;
		try {
			inNode = (IncompleteNode) inNodes.readObject();
			inEdge = (IncompleteEdge) inEdges.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		node = new Node(inNode.id,inNode.lat,inNode.lon);
		
		// Loop through the two files until one runs out
		while(true) {
			
			if(inEdge.nodeID1 == node.id) {
				edge = new Edge(inEdge.nodeID2,inEdge.type,inEdge.distance);
				node.addEdge(edge);
				try {
					inEdge = (IncompleteEdge) inEdges.readObject();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					if(node.edges.size() > 0) {
						try {
							out.writeObject(node);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					break;
				}
			}
			else if(inEdge.nodeID1 < node.id) {
				try {
					inEdge = (IncompleteEdge) inEdges.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					if(node.edges.size() > 0) {
						try {
							out.writeObject(node);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					break;
				}
			}
			else {
				try {
					if(node.edges.size() > 0) {
						out.writeObject(node);
					}
					inNode = (IncompleteNode) inNodes.readObject();
					node = new Node(inNode.id,inNode.lat,inNode.lon);
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of nodes, break
					break;
				}
			}
		}
		
		// Clean up
		try {
			out.flush();
			out.close();
			inNodes.close();
			inEdges.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
