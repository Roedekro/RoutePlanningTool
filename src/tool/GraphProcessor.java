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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import Address.AdrNode;
import Address.AdrNodeCollection;
import Address.AngleEdge;
import Address.IncompleteAdrEdge;
import Address.IncompleteAdrNode;
import Address.K2Tree;
import Address.Range;

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
			node = (IncompleteNode) inNodes.readUnshared();
			edge = (IncompleteEdge) inEdges.readUnshared();
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
					outEdges.writeUnshared(edge);
					//outEdges.reset();
					edge = (IncompleteEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {
						try {
							outNodes.writeUnshared(node);
							//outNodes.reset();
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else if(edge.nodeID1 < node.id) {
				try {
					edge = (IncompleteEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						try {
							outNodes.writeUnshared(node);
							//outNodes.reset();
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else {
				try {
					outNodes.writeUnshared(node);
					//outNodes.reset();
					node = (IncompleteNode) inNodes.readUnshared();
					//inNodes.reset();
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
			node = (IncompleteNode) inNodes.readUnshared();
			edge = (IncompleteEdge) inEdges.readUnshared();
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
						outEdges.writeUnshared(new IncompleteEdge(edge.nodeID2,edge.nodeID1,edge.type,distance,edge.maxSpeed,edge.travelTime));
						//outEdges.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				try {
					outEdges.writeUnshared(edge);
					//outEdges.reset();
					edge = (IncompleteEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {
						if(node.pointedFromTo) {
							try {
								outNodes.writeUnshared(node);
								//outNodes.reset();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						try {
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else if(edge.nodeID2 < node.id) {
				try {
					edge = (IncompleteEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						if(node.pointedFromTo) {
							try {
								outNodes.writeUnshared(node);
								//outNodes.reset();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						try {
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
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
						outNodes.writeUnshared(node);
						//outNodes.reset();
					}
					node = (IncompleteNode) inNodes.readUnshared();
					//inNodes.reset();
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
			inNode = (IncompleteNode) inNodes.readUnshared();
			inEdge = (IncompleteEdge) inEdges.readUnshared();
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
					inEdge = (IncompleteEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					/*if(node.edges.size() > 0) {
						try {
							out.writeUnshared(node);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}*/
					while(true) {
						try {
							numberNodesOut++;
							numberEdgesOut += node.edges.size();
							out.writeUnshared(node);
							//out.reset();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							inNode = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
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
					inEdge = (IncompleteEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					/*if(node.edges.size() > 0) {
						try {
							out.writeUnshared(node);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}*/
					while(true) {
						try {
							numberNodesOut++;
							numberEdgesOut += node.edges.size();
							out.writeUnshared(node);
							//out.reset();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							inNode = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
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
						out.writeUnshared(node);
					}*/
					numberNodesOut++;
					numberEdgesOut += node.edges.size();
					out.writeUnshared(node);
					//out.reset();
					inNode = (IncompleteNode) inNodes.readUnshared();
					//inNodes.reset();
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

	/**
	 * Runs through edges by nodeID2 and filters out any that doesnt have a node present.
	 * Marks nodes with pointed to/from. Populates the lon/lats of the edges.
	 *@param nodesInput File containing sorted IncompleteNodes
	 * @param edgesInput File containing IncompleteEdges sorted by nodeID2
	 * @param nodesOutput File that will contain the filtered sorted list of IncompleteNodes
	 * @param edgesOutput File that will contain the filtered sorted list of IncompleteEdges
	 */
	protected void alternativeFirstPass(String nodesInput, String edgesInput, String nodesOutput, String edgesOutput) {
		
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
			node = (IncompleteNode) inNodes.readUnshared();
			edge = (IncompleteEdge) inEdges.readUnshared();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		
		// Loop through the two files until one runs out
		while(true) {
			
			if(edge.nodeID2 == node.id) {
				
				edge.lat = node.lat;
				edge.lon = node.lon;
				node.pointedFromTo = true;
							
				try {
					outEdges.writeUnshared(edge);
					//outEdges.reset();
					edge = (IncompleteEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {

						try {
							outNodes.writeUnshared(node);
							//outNodes.reset();
						} catch (IOException e1) {
							e1.printStackTrace();
						}

						try {
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else if(edge.nodeID2 < node.id) {
				try {
					edge = (IncompleteEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						try {
							outNodes.writeUnshared(node);
							//outNodes.reset();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						try {
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else {
				try {
					outNodes.writeUnshared(node);
					node = (IncompleteNode) inNodes.readUnshared();
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
	 * Runs through edges by nodeID1 and filters out any that doesnt have a node present.
	 * Then adds the edges to the nodes and writes out nodes pointed to/from.
	 * @param nodes File containing sorted IncompleteNodes
	 * @param edges File containing IncompleteEdges sorted by nodeID1
	 * @param output File that will contain the finished sorted list of Nodes with Edges
	 */
	protected void alternativeSecondPass(String nodes, String edges, String output) {
		
		numberNodesOut = 0;
		numberEdgesOut = 0;
		
		//long numberOfEdgesIn = 0;
		
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
		
		//numberOfEdgesIn++;
		
		IncompleteNode inNode = null;
		IncompleteEdge inEdge = null;
		Node node = null;
		Edge edge = null;
		try {
			inNode = (IncompleteNode) inNodes.readUnshared();
			inEdge = (IncompleteEdge) inEdges.readUnshared();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		node = new Node(inNode.id,inNode.lat,inNode.lon);
		
		// Loop through the two files until one runs out
		while(true) {
			
			if(inEdge.nodeID1 == node.id) {
				if(inEdge.nodeID2 != node.id) {
					inNode.pointedFromTo = true;
					// No pointing to yourself
					edge = new Edge(inEdge.nodeID2,inEdge.type,inEdge.distance,inEdge.maxSpeed,inEdge.travelTime);
					// Calculate the distance
					int distance = calculateDistance(node.lat, node.lon, inEdge.lat, inEdge.lon);
					edge.distance = distance;
					int travelTime = calculateTravelTime(distance, edge.maxSpeed, edge.type);
					edge.travelTime = travelTime;
					node.addEdge(edge);
				}
				try {
					inEdge = (IncompleteEdge) inEdges.readUnshared();
					//numberOfEdgesIn++;
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {
						try {
							if(inNode.pointedFromTo) {
								numberNodesOut++;
								numberEdgesOut += node.edges.size();
								out.writeUnshared(node);
							}
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						try {
							inNode = (IncompleteNode) inNodes.readUnshared();
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
					inEdge = (IncompleteEdge) inEdges.readUnshared();
					//numberOfEdgesIn++;
					//inEdges.reset();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						try {
							if(inNode.pointedFromTo) {
								numberNodesOut++;
								numberEdgesOut += node.edges.size();
								out.writeUnshared(node);
							}
							
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							inNode = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
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
					if(inNode.pointedFromTo) {
						numberNodesOut++;
						numberEdgesOut += node.edges.size();
						out.writeUnshared(node);
					}
					inNode = (IncompleteNode) inNodes.readUnshared();
					node = new Node(inNode.id,inNode.lat,inNode.lon);
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of nodes, break
					break;
				}
			}
		}
		
		//System.out.println("Number Edges in = "+numberOfEdgesIn);
		
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
	 * First pass that compares nodeID1 of edges with the nodes,
	 * removing any edge that doesnt have a corresponding node.
	 * Also populates the lat and lon of the edges.
	 * Marks if a node has an edge pointing to it.
	 * This method handles incomplete edges containing street names.
	 * @param nodesInput File containing sorted IncompleteNodes
	 * @param edgesInput File containing IncompleteAdrEdges sorted by nodeID1
	 * @param nodesOutput File that will contain the filtered sorted list of IncompleteNodes
	 * @param edgesOutput File that will contain the filtered sorted list of IncompleteAdrEdges
	 */
	protected void firstPassAddress(String nodesInput, String edgesInput, String nodesOutput, String edgesOutput) {
		
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
		IncompleteAdrEdge edge = null;
		try {
			node = (IncompleteNode) inNodes.readUnshared();
			edge = (IncompleteAdrEdge) inEdges.readUnshared();
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
					outEdges.writeUnshared(edge);
					//outEdges.reset();
					edge = (IncompleteAdrEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {
						try {
							outNodes.writeUnshared(node);
							//outNodes.reset();
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else if(edge.nodeID1 < node.id) {
				try {
					edge = (IncompleteAdrEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						try {
							outNodes.writeUnshared(node);
							//outNodes.reset();
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else {
				try {
					outNodes.writeUnshared(node);
					//outNodes.reset();
					node = (IncompleteNode) inNodes.readUnshared();
					//inNodes.reset();
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
	 * This method handles incomplete edges having street names, and will
	 * calculate the angle of an edge.
	 * @param nodesInput File containing sorted IncompleteNodes
	 * @param edgesInput File containing IncompleteAdrEdges sorted by nodeID2
	 * @param nodesOutput File containing sorted list of IncompleteNodes that has an edge pointing from/to it.
	 * @param edgesOutput File that will contain the finished (NOT sorted by ID1) list of IncompleteAdrEdges
	 */
	protected void secondPassAddress(String nodesInput, String edgesInput, String nodesOutput, String edgesOutput) {
		
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
		IncompleteAdrEdge edge = null;
		try {
			node = (IncompleteNode) inNodes.readUnshared();
			edge = (IncompleteAdrEdge) inEdges.readUnshared();
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
				
				// Calculate the angle
				int angle = calculateAngle(node.lat, node.lon, edge.lat, edge.lon);
				edge.angle = angle;
				
				// If not oneway add a copy with switched nodeID1 and nodeID2
				// The angle is obv. reverse
				if(!edge.oneway) {
					try {
						int oppositeAngle;
						if(angle < 180) {
							oppositeAngle = angle + 180;
						}
						else if( angle > 180) {
							oppositeAngle = angle - 180;
						}
						else {
							oppositeAngle = 0;
						}
						outEdges.writeUnshared(new IncompleteAdrEdge(edge.nodeID2,edge.nodeID1,edge.type,distance,edge.maxSpeed,edge.travelTime,edge.streetName,oppositeAngle));
						//outEdges.reset();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				try {
					outEdges.writeUnshared(edge);
					//outEdges.reset();
					edge = (IncompleteAdrEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {
						if(node.pointedFromTo) {
							try {
								outNodes.writeUnshared(node);
								//outNodes.reset();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						try {
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else if(edge.nodeID2 < node.id) {
				try {
					edge = (IncompleteAdrEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						if(node.pointedFromTo) {
							try {
								outNodes.writeUnshared(node);
								//outNodes.reset();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						try {
							node = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
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
						outNodes.writeUnshared(node);
						//outNodes.reset();
					}
					node = (IncompleteNode) inNodes.readUnshared();
					//inNodes.reset();
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
	 * Third pass that compares adds creates Nodes and adds IncompleteEdges
	 * to these nodes.
	 * The result is a list of AdrNodes WITHOUT streetname and housenumber!
	 * The AngleEdges will contain streetnames that can be used to determine
	 * a streetname, if any, for the node.
	 * Housenumbers is contained in seperate file of IncompleteAdrNode.
	 * @param nodes File containing sorted IncompleteNodes
	 * @param edges File containing IncompleteEdges sorted by nodeID1
	 * @param output File that will contain a sorted list of AdrNodes with AngleEdges
	 */
	protected void thirdPassAddress(String nodes, String edges, String output) {
		
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
		IncompleteAdrEdge inEdge = null;
		AdrNode node = null;
		AngleEdge edge = null;
		try {
			inNode = (IncompleteNode) inNodes.readUnshared();
			inEdge = (IncompleteAdrEdge) inEdges.readUnshared();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		node = new AdrNode(inNode.id,inNode.lat,inNode.lon);
		
		// Loop through the two files until one runs out
		while(true) {
			
			if(inEdge.nodeID1 == node.id) {
				if(inEdge.nodeID2 != node.id) {
					// No pointing to yourself
					edge = new AngleEdge(inEdge.nodeID2,inEdge.distance,inEdge.type,inEdge.angle,inEdge.streetName);
					node.addEdge(edge);
				}
				try {
					inEdge = (IncompleteAdrEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (IOException | ClassNotFoundException e) {
					// Ran out of edges, break
					while(true) {
						try {
							numberNodesOut++;
							numberEdgesOut += node.edges.size();
							out.writeUnshared(node);
							//out.reset();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							inNode = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
							node = new AdrNode(inNode.id,inNode.lat,inNode.lon);
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}		
					break;
				}
			}
			else if(inEdge.nodeID1 < node.id) {
				try {
					inEdge = (IncompleteAdrEdge) inEdges.readUnshared();
					//inEdges.reset();
				} catch (ClassNotFoundException | IOException e) {
					// Ran out of edges, break
					while(true) {
						try {
							numberNodesOut++;
							numberEdgesOut += node.edges.size();
							out.writeUnshared(node);
							//out.reset();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							inNode = (IncompleteNode) inNodes.readUnshared();
							//inNodes.reset();
							node = new AdrNode(inNode.id,inNode.lat,inNode.lon);
						} catch (ClassNotFoundException | IOException e1) {
							break;
						}
					}
					break;
				}
			}
			else {
				try {
					numberNodesOut++;
					numberEdgesOut += node.edges.size();
					out.writeUnshared(node);
					//out.reset();
					inNode = (IncompleteNode) inNodes.readUnshared();
					//inNodes.reset();
					node = new AdrNode(inNode.id,inNode.lat,inNode.lon);
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
	
	/*
	 * Will return the angle between two lat/lon points in degrees
	 * between 0 and 359.
	 * Note that the angle (or bearing) is only accurate for small distances.
	 * The bearing between two long range points is not a constant.
	 */
	private int calculateAngle(double inlat, double inlon, double inlat2, double inlon2) {
		
		// Convert to radians
		double lat = Math.toRadians(inlat);
		double lon = Math.toRadians(inlon);
		double lat2 = Math.toRadians(inlat2);
		double lon2 = Math.toRadians(inlon2);
		
		double deltaLon = lon2 - lon;
		
		double angle = Math.atan2(Math.sin(deltaLon)*Math.cos(lat2), Math.cos(lat)*Math.sin(lat2) - 
				Math.sin(lat)*Math.cos(lat2)*Math.cos(deltaLon));
		
		angle = Math.toDegrees(angle); // Convert radians to degrees
		angle = (angle + 360) % 360; // Could be minus
		int ret = (int) Math.round(angle); // Can round up to 360.
		if(ret == 360) {
			ret = 0;
		}
		return ret;
	}
	
	/*
	 * Reads in a series of AdrNodes, containing AngleEdges.
	 * Uses the edges to assign a streetname to a node, if possible.
	 * Next we take any node with a street name and insert them in a 2D KD-Tree.
	 * Using the KD-Tree we assign house numbers to the nodes.
	 * The final house number will be the median of assigned house numbers.
	 */
	protected void fourthPassAddress(String innodes, String inaddresses, String output) {
		
		ArrayList<AdrNode> nodes = new ArrayList<AdrNode>();
		HashMap<Long,AdrNode> map = new HashMap<Long,AdrNode>();
		
		// Read in Nodes
		ObjectInputStream inNodes = null;
		
		try {
			inNodes = new ObjectInputStream(new BufferedInputStream(new FileInputStream(innodes),B));;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		AdrNode inNode;
		
		System.out.println("Building array and hashmap");
		// Build up array and hashmap
		while(true) {
			try {
				inNode = (AdrNode) inNodes.readUnshared();
				inNode.reverseEdges = new ArrayList<AngleEdge>();
				nodes.add(inNode);
				map.put(inNode.id, inNode);
			} catch (IOException | ClassNotFoundException e) {
				break;
			}
		}
		
		System.out.println("Adding reverse edges");
		// Add reverse edges
		AdrNode source;
		AngleEdge edge;
		AdrNode destination;
		for(int i = 0; i < nodes.size(); i++) {
			source = nodes.get(i);
			for(int j = 0; j < source.edges.size(); j++) {
				edge = source.edges.get(j);
				destination = map.get(edge.nodeid);
				destination.addReverseEdge(edge);
			}
		}
		
		/*
		 * Use edges and reverse edges to determine street name.
		 * The rules are as follows.
		 * If a node only contains edges and reverse edges of one street,
		 * then obviously the node will be labeled with that street name.
		 * Otherwhise, if one street dominates (by more occurences) the other
		 * streets, the node will be labeled with that street name.
		 * If no streetname is dominant the node is an intersection belonging
		 * to multiple streets and is given no label.
		 * 
		 * The reasoning behind the rules is that we will later assign house
		 * numbers. A node intersecting two streets will not have a unique house
		 * number, as it may belong to either street. 
		 * On the other hand a T-Section does belong to one main road.
		 * A T-Section will, under the rules listed above, be labeled with a
		 * street name, as one street will dominate in the number of occurrences
		 * in the forward and reverse edge lists.
		 * 
		 * The reason we make the effort of counting reverse edges is due to
		 * oneway streets.
		 */
		
		System.out.println("Calculating streetnames");
		AdrNode node;
		ArrayList<String> names;
		ArrayList<Integer> counters;
		for(int i = 0; i < nodes.size(); i++) {
			names = new ArrayList<String>();
			counters = new ArrayList<Integer>();
			node = nodes.get(i);
			for(int j = 0; j < node.edges.size(); j++) {
				edge = node.edges.get(j);
				if(names.contains(edge.street)) {
					int index = names.indexOf(edge.street);
					int counter = counters.get(index);
					counter++;
				}
				else {
					names.add(edge.street);
					counters.add(1);
				}
			}
			
			for(int j = 0; j < node.reverseEdges.size(); j++) {
				edge = node.reverseEdges.get(j);
				if(names.contains(edge.street)) {
					int index = names.indexOf(edge.street);
					int counter = counters.get(index);
					counter++;
				}
				else {
					names.add(edge.street);
					counters.add(1);
				}
			}
			
			// Determine if one street dominates the node
			if(names.size() == 1) {
				node.street = names.get(0);
			}
			else {
				int index = 0;
				int highest = 0;
				boolean dominates = false;
				for(int j = 0; j < counters.size(); j++) {
					if(counters.get(j) > highest) {
						highest = counters.get(j);
						dominates = true;
						index = j;
					}
					else if(counters.get(j) == highest) {
						dominates = false;
					}
				}
				
				if(dominates) {
					node.street = names.get(index);
				}
				else {
					node.street = "";
				}
			}
		}
		
		// Insert nodes with street name into KD-Tree
		ArrayList<AdrNode> addressNodes = new ArrayList<AdrNode>();
		for(int i = 0; i < nodes.size(); i++) {
			node = nodes.get(i);
			if(node.street != "") {
				node.houseNumbers = new ArrayList<Integer>();
				addressNodes.add(node);
			}
		}
		
		System.out.println("Building K2-Tree");
		AdrNodeCollection col = new AdrNodeCollection(addressNodes);
		K2Tree tree = new K2Tree(col,1,null,new Range(Double.MIN_VALUE,Double.MAX_VALUE,
				Double.MIN_VALUE,Double.MAX_VALUE));
		
		
		System.out.println("Reading in addresses");
		// Read in addresses
		ObjectInputStream inAdr = null;
		
		try {
			inAdr = new ObjectInputStream(new BufferedInputStream(new FileInputStream(inaddresses),B));;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		IncompleteAdrNode address;
		ArrayList<IncompleteAdrNode> addresses = new ArrayList<IncompleteAdrNode>();
		
		while(true) {
			try {
				address = (IncompleteAdrNode) inAdr.readUnshared();
				addresses.add(address);
			} catch (IOException | ClassNotFoundException e) {
				break;
			}
		}
		
		System.out.println("Assigning addresses to road network nodes");
		// Assign addresses to nodes
		// We do this by searching for all road-network nodes in an area centered
		// around the given address. We then determine the closest of these points
		// that match the street name, and add the house number to this node.
		// We abuse that 25m is roughly 0.00022500022 degrees in lat and lon.
		double offset = 0.00022500022;
		for(int i = 0; i < addresses.size(); i++) {
			address = addresses.get(i);
			Range range = new Range(address.lat-offset,address.lat+offset,
					address.lon-offset,address.lon+offset);
			ArrayList<AdrNode> candidates = tree.query(range);
			if(candidates.size() == 0) {
				// Do nothing
			}
			else if(candidates.size() == 1) {
				if(candidates.get(0).street.equalsIgnoreCase(address.streetName)) {
					candidates.get(0).addHousenumber(address.houseNumber);
				}
			}
			else {
				// Multiple candidates
				int bestCandidate = -1;
				double closest = 1000;
				for(int j = 0; j < candidates.size(); j++) {
					node = candidates.get(j);
					if(node.street.equalsIgnoreCase(address.streetName)) {
						// Rough estimate, because we are within 25m its okay.
						double distance = Math.sqrt(Math.pow(Math.abs(node.lat - address.lat), 2) 
								+ Math.pow(Math.abs(node.lon - address.lon), 2));
						if( distance < closest) {
							bestCandidate = j;
							closest = distance;
						}
					}
				}
				if(bestCandidate != -1) {
					node = candidates.get(bestCandidate);
					node.addHousenumber(address.houseNumber);
				}
			}
		}
		
		System.out.println("Finding median house numbers");
		for(int i = 0; i < nodes.size(); i++) {
			node = nodes.get(i);
			if(node.houseNumbers.size() != 0) {
				if(node.houseNumbers.size() == 1) {
					node.house = node.houseNumbers.get(0);
				}
				else {
					// Get median
					Collections.sort(node.houseNumbers);
					node.house = node.houseNumbers.get(node.houseNumbers.size()/2);
				}
			}
			else {
				node.house = -1;
			}
		}
		
		System.out.println("Writing output");
		// Export as XML
		OutputStream out;
		XMLOutputFactory fac;
		XMLStreamWriter writer;
		try {
			out = new FileOutputStream(new File(output));
			fac = XMLOutputFactory.newFactory();
			writer = fac.createXMLStreamWriter(out);
			
			writer.writeStartDocument();
			
			for(int i = 0; i < nodes.size(); i++) {
				node = nodes.get(i);
				writer.writeStartElement("Node");
				writer.writeAttribute("id", Long.toString(node.id));
				writer.writeAttribute("lat", Double.toString(node.lat));
				writer.writeAttribute("lon", Double.toString(node.lon));
				if(!node.street.equalsIgnoreCase("")) {
					writer.writeAttribute("street", node.street);
					if(node.house != -1) {
						writer.writeAttribute("house", Integer.toString(node.house));
					}
				}
				for(int j = 0; j < node.edges.size(); j++) {
					edge = node.edges.get(j);
					writer.writeStartElement("Edge");
					writer.writeAttribute("destination", Long.toString(edge.nodeid));
					writer.writeAttribute("weight", Integer.toString(edge.weight));
					writer.writeAttribute("bearing", Integer.toString(edge.angle));
					writer.writeEndElement();
				}
				
				writer.writeEndElement();
			}
			
			writer.writeEndDocument();
			writer.flush();
			writer.close();
		} catch (FileNotFoundException | XMLStreamException e) {
			e.printStackTrace();
		}
		
		System.out.println("Done");
	}
}
