package tool;
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
class GraphProcessor {
	
	protected long M = 2000000000;
	protected int B = 4096;
	protected long numberNodesOut = 0;
	protected long numberEdgesOut = 0;
	
	protected GraphProcessor(long M, int B) {
		if(M > 0) this.M = M;
		if(B > 0) this.B = B;
	}
	
	/**
	 * First pass that compares nodeID1 of edges with the nodes,
	 * removing any edge that doesnt have a corresponding node.
	 * Also populates the lat and lon of the edges.
	 * Marks if a node has an edge pointing to it.
	 * @param nodesInput File containing sorted IncompleteNodes
	 * @param edgesInput File containing IncompleteEdges sorted by nodeID1
	 * @param nodesOutput File that will contain the filtered sorted list of IncompleteNodes
	 * @param edgesOutput File that will contain the filtered sorted list of IncompleteEdges
	 */
	protected void firstPassCombineIncompleteNodeEdge(String nodesInput, String edgesInput, String nodesOutput, String edgesOutput) {
		
		ObjectInputStream inNodes = null;
		ObjectInputStream inEdges = null;
		ObjectOutputStream outNodes = null;
		ObjectOutputStream outEdges = null;
		
		try {
			inNodes = new ObjectInputStream(new BufferedInputStream(new FileInputStream(nodesInput),B));
			inEdges = new ObjectInputStream(new BufferedInputStream(new FileInputStream(edgesInput),B));
			outNodes = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(nodesOutput),B));
			outEdges = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(edgesOutput),B));
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
				node.pointedFromTo = true;
				try {
					outEdges.writeObject(edge);
					edge = (IncompleteEdge) inEdges.readObject();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {
						try {
							outNodes.writeObject(node);
							node = (IncompleteNode) inNodes.readObject();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else if(edge.nodeID1 < node.id) {
				try {
					edge = (IncompleteEdge) inEdges.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						try {
							outNodes.writeObject(node);
							node = (IncompleteNode) inNodes.readObject();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else {
				try {
					outNodes.writeObject(node);
					node = (IncompleteNode) inNodes.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of nodes, break
					break;
				}
			}
		}
		
		// Clean up
		try {
			outNodes.flush();
			outNodes.close();
			outEdges.flush();
			outEdges.close();
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
	 * Removes any node that doesnt have an edge pointing from/to it.
	 * @param nodesInput File containing sorted IncompleteNodes
	 * @param edgesInput File containing IncompleteEdges sorted by nodeID2
	 * @param nodesOutput File containing sorted list of IncompleteNodes that has an edge pointing from/to it.
	 * @param edgesOutput File that will contain the finished (NOT sorted by ID1) list of IncompleteEdges
	 */
	protected void secondPassCombineIncompleteNodeEdge(String nodesInput, String edgesInput, String nodesOutput, String edgesOutput) {
		
		ObjectInputStream inNodes = null;
		ObjectInputStream inEdges = null;
		ObjectOutputStream outNodes = null;
		ObjectOutputStream outEdges = null;
		
		try {
			inNodes = new ObjectInputStream(new BufferedInputStream(new FileInputStream(nodesInput),B));
			inEdges = new ObjectInputStream(new BufferedInputStream(new FileInputStream(edgesInput),B));
			outNodes = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(nodesOutput),B));
			outEdges = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(edgesOutput),B));
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
				
				node.pointedFromTo = true;
				
				// Calculate the distance
				int distance = calculateDistance(node.lat, node.lon, edge.lat, edge.lon);
				edge.distance = distance;
				int travelTime = calculateTravelTime(distance, edge.maxSpeed, edge.type);
				edge.travelTime = travelTime;
				
				// If not oneway add a copy with switched nodeID1 and nodeID2
				if(!edge.oneway) {
					try {
						outEdges.writeObject(new IncompleteEdge(edge.nodeID2,edge.nodeID1,edge.type,distance,edge.maxSpeed,edge.travelTime));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				try {
					outEdges.writeObject(edge);
					edge = (IncompleteEdge) inEdges.readObject();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {
						if(node.pointedFromTo) {
							try {
								outNodes.writeObject(node);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						try {
							node = (IncompleteNode) inNodes.readObject();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else if(edge.nodeID2 < node.id) {
				try {
					edge = (IncompleteEdge) inEdges.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						if(node.pointedFromTo) {
							try {
								outNodes.writeObject(node);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						try {
							node = (IncompleteNode) inNodes.readObject();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else {
				try {
					if(node.pointedFromTo) {
						outNodes.writeObject(node);
					}
					node = (IncompleteNode) inNodes.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of nodes, break
					break;
				}
			}
		}
		
		// Clean up
		try {
			outNodes.flush();
			outNodes.close();
			outEdges.flush();
			outEdges.close();
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
	 * @return distance in cm
	 */
	protected int calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		
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
		
		return (int) Math.round(distance*100000); // Cm because Math.round is long
		
	}
	
	/**
	 * Third pass that compares adds creates Nodes and adds IncompleteEdges
	 * to these nodes.
	 * @param nodes File containing sorted IncompleteNodes
	 * @param edges File containing IncompleteEdges sorted by nodeID1
	 * @param output File that will contain the finished sorted list of Nodes with Edges
	 */
	protected void thirdPassCombineIncompleteNodeEdge(String nodes, String edges, String output) {
		
		numberNodesOut = 0;
		numberEdgesOut = 0;
		
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
				if(inEdge.nodeID2 != node.id) {
					// No pointing to yourself
					edge = new Edge(inEdge.nodeID2,inEdge.type,inEdge.distance,inEdge.maxSpeed,inEdge.travelTime);
					node.addEdge(edge);
				}
				try {
					inEdge = (IncompleteEdge) inEdges.readObject();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					/*if(node.edges.size() > 0) {
						try {
							out.writeObject(node);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}*/
					while(true) {
						try {
							numberNodesOut++;
							numberEdgesOut += node.edges.size();
							out.writeObject(node);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							inNode = (IncompleteNode) inNodes.readObject();
							node = new Node(inNode.id,inNode.lat,inNode.lon);
						} catch (ClassNotFoundException | IOException e1) {
							break;
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
					/*if(node.edges.size() > 0) {
						try {
							out.writeObject(node);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}*/
					while(true) {
						try {
							numberNodesOut++;
							numberEdgesOut += node.edges.size();
							out.writeObject(node);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							inNode = (IncompleteNode) inNodes.readObject();
							node = new Node(inNode.id,inNode.lat,inNode.lon);
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else {
				try {
					/* Moved functionality to second pass
					if(node.edges.size() > 0) {
						out.writeObject(node);
					}*/
					numberNodesOut++;
					numberEdgesOut += node.edges.size();
					out.writeObject(node);
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
	
	/**
	 * Calculates travel time over a distance in milliseconds
	 * @param distance cm
	 * @param maxSpeed km/h
	 * @param type highway type, in case maxspeed=0
	 * @return travel time in milliseconds
	 */
	protected int calculateTravelTime(int distance, int maxSpeed, String type) {
		int max = maxSpeed;
		if(maxSpeed == 0) {
			if(type.equalsIgnoreCase("motorway")) {
				max = 130;
			}
			else if(type.equalsIgnoreCase("trunk")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("primary")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("secondary")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("tertiary")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("unclassified")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("residential")) {
				max = 50;
			}
			else if(type.equalsIgnoreCase("service")) {
				max = 50;
			}
			else if(type.equalsIgnoreCase("motorway_link")) {
				max = 130;
			}
			else if(type.equalsIgnoreCase("trunk_link")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("primary_link")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("secondary_link")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("tertiary_link")) {
				max = 80;
			}
			else if(type.equalsIgnoreCase("living_street")) {
				max = 50;
			}
			else if(type.equalsIgnoreCase("road")) {
				max = 80;
			}
			else {
				return 0;
			}
		}
		// Distance / speed in cm/second gives travel time in seconds, times one thousand for milliseconds
		return (int) Math.round((distance / (max * 27.7777778)) * 1000);
	}

}
