package tool;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Class responsible for validating Node files vs. the original .osm file.
 * @author Martin
 *
 */
class Validator {

	protected int B = 4096;
	
	protected Validator(int B) {
		this.B = B;
	}
	
	/**
	 * Validates a Node file up against a .osm file. It assumes the .osm file is already boxed, 
	 * but not filtered. The filters will be assumed to be that of the car filter.
	 * @param nodes File of Nodes.
	 * @param osm export file.
	 * @return True if no errors are detected in the Node file.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws XMLStreamException 
	 */
	protected boolean validate(String nodes, String osm) throws FileNotFoundException, IOException, XMLStreamException {
		
		boolean ret = true; // Set false if error
		
		// Load in the Nodes and place them in a hashmap
		HashMap<Long, Node> hashMap = new HashMap<Long, Node>();
		ObjectInputStream nodeIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(nodes),B));
		
		Node node = null;
		while(true) {
			try {
				node = (Node) nodeIn.readObject();
				hashMap.put(node.id, node);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				break;
			}
		}
		
		nodeIn.close();

		// Now run through the .osm file and check all Ways.
		InputStream	is = new FileInputStream(new File(osm));
		XMLInputFactory	xif = XMLInputFactory.newInstance();
		XMLEventReader reader = xif.createXMLEventReader(is);

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
		
		boolean insideWay = false;
		long lastWayNode = 0;
		long firstWayNode = 0;
		String v = "";
		ArrayList<IncompleteEdge> edgeList = new ArrayList<IncompleteEdge>();
		while(reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if(event.getEventType() == XMLStreamConstants.START_ELEMENT) {
				StartElement start = event.asStartElement();
				String type = start.getName().getLocalPart();
				if(type.equalsIgnoreCase("way")) {
					insideWay = true;
					lastWayNode = 0;
					edgeList = new ArrayList<IncompleteEdge>();
				}
				else if(type.equalsIgnoreCase("nd")) {
					if(insideWay) {
						long val = Long.parseLong(((Attribute)start.getAttributes().next()).getValue());
						if(lastWayNode == 0) {
							firstWayNode = val;
							lastWayNode = val;
						}
						else {
							//System.out.println("Edge from "+lastWayNode+" to "+val);
							edgeList.add(new IncompleteEdge(lastWayNode,val));
							lastWayNode = val;
						}
					}
				}
				else if(type.equalsIgnoreCase("tag")) {
					if(insideWay) {
						Iterator attributes = start.getAttributes();
						// It checks attributes from the back to the front
						// Which means it checks v first, so save v
						// and use it if k=highway
						while(attributes.hasNext()) {
							Attribute attribute = (Attribute) attributes.next();
							if(attribute.getName().getLocalPart().equalsIgnoreCase("k")) {
								if(attribute.getValue().equalsIgnoreCase("highway")) {
									for(int i = 0; i < edgeList.size(); i++) {
										edgeList.get(i).type = v;
									}
									// Just to be sure
									if(v.equalsIgnoreCase("motorway")) {
										for(int i = 0; i < edgeList.size(); i++) {
											edgeList.get(i).oneway = true;
										}
									}
									else if(v.equalsIgnoreCase("motorway_link")) {
										for(int i = 0; i < edgeList.size(); i++) {
											edgeList.get(i).oneway = true;
										}
									}
								}
								else if(attribute.getValue().equalsIgnoreCase("oneway")) {
									if(v.equalsIgnoreCase("yes")) {
										for(int i = 0; i < edgeList.size(); i++) {
											edgeList.get(i).oneway = true;
										}
									}
								}
								else if(attribute.getValue().equalsIgnoreCase("junction")) {
									if(v.equalsIgnoreCase("roundabout")) {
										edgeList.add(new IncompleteEdge(lastWayNode, firstWayNode, edgeList.get(0).type));
										for(int i = 0; i < edgeList.size(); i++) {
											edgeList.get(i).oneway = true;
										}
									}
								}
								else if(attribute.getValue().equalsIgnoreCase("maxspeed")) {
									int maxspeed = Integer.parseInt(v);
									for(int i = 0; i < edgeList.size(); i++) {
										edgeList.get(i).maxSpeed = maxspeed;
									}
								}
							}
							else if(attribute.getName().getLocalPart().equalsIgnoreCase("v")) {
								v = attribute.getValue();
							}	
						}
					}
				}
				else if(type.equalsIgnoreCase("relation")) {
					// Done with ways, break
					break;
				}
			}
			else if(event.getEventType() == XMLStreamConstants.END_ELEMENT) {
				EndElement end = event.asEndElement();
				String type = end.getName().getLocalPart();
				if(type.equalsIgnoreCase("way")) {
					insideWay = false;
					boolean filter = false;
					if(edgeList.size() > 0) {
						String edgeType = edgeList.get(0).type;
						for(int i = 0; i < filters.size(); i++) {
							if(edgeType.equalsIgnoreCase(filters.get(i))) {
								filter = true;
								break;
							}
						}
					}
					else {
						// No filter arguments were given
						filter = true;
					}
					if(filter) {
						IncompleteEdge edge = null;
						for(int i = 0; i < edgeList.size(); i++) {
							edge = null;
							node = null;
							edge = edgeList.get(i);
							if(edge.nodeID1 == edge.nodeID2) {
								// These will be weeded out of the Node file.
								continue;
							}
							node = hashMap.get(edge.nodeID1);
							if(node == null) {
								ret = false;
								System.out.println("Missing Node "+edge.nodeID1);
							}
							else {
								Edge nodeEdge = null;
								for(int j = 0; j < node.edges.size(); j++) {
									if(node.edges.get(j).nodeID == edge.nodeID2) {
										nodeEdge = node.edges.get(j);
									}
								}
								if(nodeEdge == null) {
									ret = false;
									System.out.println("Missing Edge from "+node.id+" to "+edge.nodeID2);
								}
								else {
									if(edge.maxSpeed != nodeEdge.maxSpeed) {
										ret = false;
										System.out.println("Wrong maxSpeed on Edge from "+node.id+" to "+edge.nodeID2);
									}
									if(!edge.type.equalsIgnoreCase(nodeEdge.type)) {
										ret = false;
										System.out.println("Wrong type on Edge from "+node.id+" to "+edge.nodeID2);
									}
									Edge reverseEdge = null;
									Node secondNode = null;
									secondNode = hashMap.get(edge.nodeID2);			
									if(secondNode == null) {
										ret = false;
										System.out.println("Missing Node "+edge.nodeID2);
									}
									else {
										// Try and find the reverse node
										for(int j = 0; j < secondNode.edges.size(); j++) {
											if(secondNode.edges.get(j).nodeID == edge.nodeID1) {
												reverseEdge = secondNode.edges.get(j);
											}
										}
										if(edge.oneway) {
											if(reverseEdge != null) {
												ret = false;
												System.out.println("Found extra Edge from "+edge.nodeID2+" to "+edge.nodeID1);
											}
										}
										else {
											// Check second edge
											if(reverseEdge == null) {
												ret = false;
												System.out.println("Missing Reverse Edge from "+edge.nodeID2+" to "+edge.nodeID1);
											}
											else {
												if(edge.maxSpeed != reverseEdge.maxSpeed) {
													ret = false;
													System.out.println("Wrong maxSpeed on Edge from "+edge.nodeID2+" to "+edge.nodeID1);
												}
												if(!edge.type.equalsIgnoreCase(reverseEdge.type)) {
													ret = false;
													System.out.println("Wrong type on Edge from "+edge.nodeID2+" to "+edge.nodeID1);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				
			}
		}
		reader.close();
		is.close();
		return ret;
	}
}
