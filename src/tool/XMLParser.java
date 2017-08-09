package tool;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.xml.sax.Attributes;

/**
 * Takes an .osm file and outputs it to a file of IncompleteNodes and IncompleteEdges.
 * @author Martin
 *
 */

class XMLParser {
	
	protected long numberNodesIn = 0;
	protected long numberWaysIn = 0;
	protected long numberNodesOut = 0;
	protected long numberEdgesOut = 0;
	
	/**
	 * Parses a .osm file
	 * @param input Name of input
	 * @param nodeOutput Name of output file for nodes
	 * @param edgeOutput Name of output file for edges
	 * @param B Block size
	 * @throws XMLStreamException
	 * @throws IOException 
	 */
	protected void parse(String input, String nodeOutput, String edgeOutput, int B) throws XMLStreamException, IOException {
		
		parseBoxAndFilterEdges(input, nodeOutput, edgeOutput, B, new ArrayList<String>(), 
				Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE);
	}
	
	/**
	 * Parses a .osm file and filter _nodes_ (not edges) by Latitude and Longitude
	 * @param input Name of input
	 * @param nodeOutput Name of output file for nodes
	 * @param edgeOutput Name of output file for edges
	 * @param B Block size
	 * @param latMin Minimum latitude
	 * @param latMax Maximum latitude
	 * @param lonMin Minimum longitude
	 * @param lonMax Maximum latitude
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	protected void parseAndBox(String input, String nodeOutput, String edgeOutput, int B, double latMin,
			double latMax, double lonMin, double lonMax) throws XMLStreamException, IOException {
		
		parseBoxAndFilterEdges(input, nodeOutput, edgeOutput, B, new ArrayList<String>(), latMin, latMax, lonMin, lonMax);	
	}
	
	/**
	 * Parses a .osm file and filter edges by highway type
	 * @param input Name of input
	 * @param nodeOutput Name of output file for nodes
	 * @param edgeOutput Name of output file for edges
	 * @param B Block size
	 * @param filters for the type of highways
	 * @throws XMLStreamException
	 * @throws IOException 
	 */
	protected void parseAndFilterEdges(String input, String nodeOutput, String edgeOutput, int B, ArrayList<String> filters) 
			throws XMLStreamException, IOException {
		
		parseBoxAndFilterEdges(input, nodeOutput, edgeOutput, B, filters, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE);
	}
	
	/**
	 * Parses a .osm file and filter _nodes_ (not edges) by Latitude and Longitude
	 * and filters edges by highway type
	 * @param input Name of input
	 * @param nodeOutput Name of output file for nodes
	 * @param edgeOutput Name of output file for edges
	 * @param B Block size
	 * @param filters for the type of highways
	 * @param minLat Minimum latitude
	 * @param maxLat Maximum latitude
	 * @param minLon Minimum longitude
	 * @param maxLon Maximum latitude
	 * @throws XMLStreamException
	 * @throws IOException 
	 */
	protected void parseBoxAndFilterEdges(String input, String nodeOutput, String edgeOutput, int B, 
			ArrayList<String> filters, double minLat,	double maxLat, double minLon, double maxLon) 
			throws XMLStreamException, IOException {
		
		numberNodesIn = 0;
		numberWaysIn = 0;
		numberNodesOut = 0;
		numberEdgesOut = 0;
		
		ObjectOutputStream outN = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(nodeOutput),B));
		ObjectOutputStream outE = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(edgeOutput),B));
		InputStream is = new FileInputStream(new File(input));
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLEventReader reader = xif.createXMLEventReader(is);
		long id = 0;
		double lon = 0, lat = 0;
		boolean insideWay = false;
		boolean address = false;
		long lastWayNode = 0;
		long firstWayNode = 0;
		String v = null;
		ArrayList<IncompleteEdge> edgeList = null; // List to store edges until we meet the highway tag
		long counter = 0;
		while(reader.hasNext()) {
			//counter++;
			//System.out.println(counter);
			XMLEvent event = reader.nextEvent();
			if(event.getEventType() == XMLStreamConstants.START_ELEMENT) {
				StartElement start = event.asStartElement();
				String type = start.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					numberNodesIn++;
					address = false;
					Iterator attributes = start.getAttributes();
					while(attributes.hasNext()) {
						Attribute attribute = (Attribute) attributes.next();
						if(attribute.getName().getLocalPart().equalsIgnoreCase("id")) {
							id = Long.parseLong(attribute.getValue());
						}
						else if(attribute.getName().getLocalPart().equalsIgnoreCase("lat")) {
							lat = Double.parseDouble(attribute.getValue());
						}
						else if(attribute.getName().getLocalPart().equalsIgnoreCase("lon")) {
							lon = Double.parseDouble(attribute.getValue());
						}						
					}
				}
				else if(type.equalsIgnoreCase("way")) {	
					numberWaysIn++;
					//System.out.println(numberWaysIn + " " + numberNodesIn);
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
									int maxspeed = 0;
									try {
										maxspeed = Integer.parseInt(v);
									}
									catch(NumberFormatException e){
										// If not an integer
										if(v.contains("urban")) {
											maxspeed = 50;
										}
										else if(v.contains("rural")) {
											maxspeed = 80;
										}
										else if(v.contains("motorway")) {
											maxspeed = 130;
										}
								    }		
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
					else {
						// Inside Node, check address
						Iterator attributes = start.getAttributes();
						while(attributes.hasNext()) {
							Attribute attribute = (Attribute) attributes.next();
							if(attribute.getName().getLocalPart().equalsIgnoreCase("k")) {
								if(attribute.getValue().equalsIgnoreCase("addr:housenumber")) {
									address = true;
								}
							}
						}
					}
				}
				else if(type.equalsIgnoreCase("relation")) {
					// Done with nodes and ways, break
					break;
				}
			}
			else if(event.getEventType() == XMLStreamConstants.END_ELEMENT) {
				EndElement end = event.asEndElement();
				String type = end.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					//System.out.println("Node: id="+id+" lat="+lat+" lon="+lon);
					if(minLat <= lat && lat <= maxLat && minLon <= lon && lon <= maxLon && !address) {
						numberNodesOut++;
						//System.out.println(numberNodesOut +" "+id);
						outN.writeUnshared(new IncompleteNode(id,lat,lon));
						//outN.writeObject(new IncompleteNode(id,lat,lon));
						//outN.reset();
					}
				}
				else if(type.equalsIgnoreCase("way")) {
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
						numberEdgesOut += edgeList.size();
						for(int i = 0; i < edgeList.size(); i++) {
							//IncompleteEdge edge = edgeList.get(i);
							//System.out.println("Edge from "+edge.nodeID1+ " to "+edge.nodeID2+" of type "+edge.type);
							outE.writeUnshared(edgeList.get(i));
							//outE.reset();
							//outE.writeObject(edgeList.get(i));
						}
					}
				}
				
			}
		}
		outN.flush();
		outN.close();
		outE.flush();
		outE.close();
		reader.close();
		is.close();
	}
	
	
}
