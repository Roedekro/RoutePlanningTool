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
public class XMLParser {
	
	/**
	 * Parses a .osm file
	 * @param input Name of input
	 * @param nodeOutput Name of output file for nodes
	 * @param edgeOutput Name of output file for edges
	 * @param B Block size
	 * @throws XMLStreamException
	 * @throws IOException 
	 */
	public void parse(String input, String nodeOutput, String edgeOutput, int B) 
			throws XMLStreamException, IOException {
		
		ObjectOutputStream outN = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(nodeOutput),B));
		ObjectOutputStream outE = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(edgeOutput),B));
		InputStream is = new FileInputStream(new File(input));
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLEventReader reader = xif.createXMLEventReader(is);
		long id = 0;
		double lon = 0, lat = 0;
		boolean insideWay = false;
		long lastWayNode = 0;
		ArrayList<IncompleteEdge> edgeList = null; // List to store edges until we meet the highway tag
		while(reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if(event.getEventType() == XMLStreamConstants.START_ELEMENT) {
				StartElement start = event.asStartElement();
				String type = start.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
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
					insideWay = true;
					lastWayNode = 0;
					edgeList = new ArrayList<IncompleteEdge>();
				}
				else if(type.equalsIgnoreCase("nd")) {
					if(insideWay) {
						long val = Long.parseLong(((Attribute)start.getAttributes().next()).getValue());
						if(lastWayNode == 0) {
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
						String v = null;
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
								}
								else if(attribute.getValue().equalsIgnoreCase("oneway")) {
									for(int i = 0; i < edgeList.size(); i++) {
										edgeList.get(i).oneway = true;
									}
								}
							}
							else if(attribute.getName().getLocalPart().equalsIgnoreCase("v")) {
								v = attribute.getValue();
							}	
						}
					}
				}
			}
			else if(event.getEventType() == XMLStreamConstants.END_ELEMENT) {
				EndElement end = event.asEndElement();
				String type = end.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					//System.out.println("Node: id="+id+" lat="+lat+" lon="+lon);
					outN.writeObject(new IncompleteNode(id,lat,lon));
				}
				else if(type.equalsIgnoreCase("way")) {
					insideWay = false;
					for(int i = 0; i < edgeList.size(); i++) {
						//IncompleteEdge edge = edgeList.get(i);
						//System.out.println("Edge from "+edge.nodeID1+ " to "+edge.nodeID2+" of type "+edge.type);
						outE.writeObject(edgeList.get(i));
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
	public void parseAndBox(String input, String nodeOutput, String edgeOutput, int B, long latMin,
			long latMax, long lonMin, long lonMax) throws XMLStreamException, IOException {
		
		ObjectOutputStream outN = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(nodeOutput),B));
		ObjectOutputStream outE = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(edgeOutput),B));
		InputStream is = new FileInputStream(new File(input));
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLEventReader reader = xif.createXMLEventReader(is);
		long id = 0;
		float lon = 0, lat = 0;
		boolean insideWay = false;
		long lastWayNode = 0;
		ArrayList<IncompleteEdge> edgeList = null; // List to store edges until we meet the highway tag
		while(reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if(event.getEventType() == XMLStreamConstants.START_ELEMENT) {
				StartElement start = event.asStartElement();
				String type = start.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					Iterator attributes = start.getAttributes();
					while(attributes.hasNext()) {
						Attribute attribute = (Attribute) attributes.next();
						if(attribute.getName().getLocalPart().equalsIgnoreCase("id")) {
							id = Long.parseLong(attribute.getValue());
						}
						else if(attribute.getName().getLocalPart().equalsIgnoreCase("lat")) {
							lat = Float.parseFloat(attribute.getValue());
						}
						else if(attribute.getName().getLocalPart().equalsIgnoreCase("lon")) {
							lon = Float.parseFloat(attribute.getValue());
						}						
					}
				}
				else if(type.equalsIgnoreCase("way")) {
					insideWay = true;
					lastWayNode = 0;
					edgeList = new ArrayList<IncompleteEdge>();
				}
				else if(type.equalsIgnoreCase("nd")) {
					if(insideWay) {
						long val = Long.parseLong(((Attribute)start.getAttributes().next()).getValue());
						if(lastWayNode == 0) {
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
						String v = null;
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
								}
								else if(attribute.getValue().equalsIgnoreCase("oneway")) {
									for(int i = 0; i < edgeList.size(); i++) {
										edgeList.get(i).oneway = true;
									}
								}
							}
							else if(attribute.getName().getLocalPart().equalsIgnoreCase("v")) {
								v = attribute.getValue();
							}	
						}
					}
				}
			}
			else if(event.getEventType() == XMLStreamConstants.END_ELEMENT) {
				EndElement end = event.asEndElement();
				String type = end.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					//System.out.println("Node: id="+id+" lat="+lat+" lon="+lon);
					if(latMin <= lat && lat <= latMax && lonMin <= lon && lon <= lonMax) {
						outN.writeObject(new IncompleteNode(id,lat,lon));
					}
				}
				else if(type.equalsIgnoreCase("way")) {
					insideWay = false;
					for(int i = 0; i < edgeList.size(); i++) {
						//IncompleteEdge edge = edgeList.get(i);
						//System.out.println("Edge from "+edge.nodeID1+ " to "+edge.nodeID2+" of type "+edge.type);
						outE.writeObject(edgeList.get(i));
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
	public void parseAndFilterEdges(String input, String nodeOutput, String edgeOutput, int B, ArrayList<String> filters) 
			throws XMLStreamException, IOException {
		
		ObjectOutputStream outN = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(nodeOutput),B));
		ObjectOutputStream outE = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(edgeOutput),B));
		InputStream is = new FileInputStream(new File(input));
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLEventReader reader = xif.createXMLEventReader(is);
		long id = 0;
		float lon = 0, lat = 0;
		boolean insideWay = false;
		long lastWayNode = 0;
		String v = null;
		ArrayList<IncompleteEdge> edgeList = null; // List to store edges until we meet the highway tag
		while(reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if(event.getEventType() == XMLStreamConstants.START_ELEMENT) {
				StartElement start = event.asStartElement();
				String type = start.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					Iterator attributes = start.getAttributes();
					while(attributes.hasNext()) {
						Attribute attribute = (Attribute) attributes.next();
						if(attribute.getName().getLocalPart().equalsIgnoreCase("id")) {
							id = Long.parseLong(attribute.getValue());
						}
						else if(attribute.getName().getLocalPart().equalsIgnoreCase("lat")) {
							lat = Float.parseFloat(attribute.getValue());
						}
						else if(attribute.getName().getLocalPart().equalsIgnoreCase("lon")) {
							lon = Float.parseFloat(attribute.getValue());
						}						
					}
				}
				else if(type.equalsIgnoreCase("way")) {
					insideWay = true;
					lastWayNode = 0;
					edgeList = new ArrayList<IncompleteEdge>();
				}
				else if(type.equalsIgnoreCase("nd")) {
					if(insideWay) {
						long val = Long.parseLong(((Attribute)start.getAttributes().next()).getValue());
						if(lastWayNode == 0) {
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
								}
							}
							else if(attribute.getName().getLocalPart().equalsIgnoreCase("v")) {
								v = attribute.getValue();
							}	
						}
					}
				}
			}
			else if(event.getEventType() == XMLStreamConstants.END_ELEMENT) {
				EndElement end = event.asEndElement();
				String type = end.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					//System.out.println("Node: id="+id+" lat="+lat+" lon="+lon);
					outN.writeObject(new IncompleteNode(id,lat,lon));
				}
				else if(type.equalsIgnoreCase("way")) {
					insideWay = false;
					boolean filter = false;
					if(edgeList.size() > 0) {
						String edgeType = edgeList.get(0).type;
						for(int i = 0; i < filters.size(); i++) {
							if(edgeType.equalsIgnoreCase(filters.get(i))) {
								filter = true;
							}
						}
					}
					if(filter) {
						for(int i = 0; i < edgeList.size(); i++) {
							//IncompleteEdge edge = edgeList.get(i);
							//System.out.println("Edge from "+edge.nodeID1+ " to "+edge.nodeID2+" of type "+edge.type);
							outE.writeObject(edgeList.get(i));
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
	
	/**
	 * Parses a .osm file and filter _nodes_ (not edges) by Latitude and Longitude
	 * and filters edges by highway type
	 * @param input Name of input
	 * @param nodeOutput Name of output file for nodes
	 * @param edgeOutput Name of output file for edges
	 * @param B Block size
	 * @param filters for the type of highways
	 * @param latMin Minimum latitude
	 * @param latMax Maximum latitude
	 * @param lonMin Minimum longitude
	 * @param lonMax Maximum latitude
	 * @throws XMLStreamException
	 * @throws IOException 
	 */
	public void parseBoxAndFilterEdges(String input, String nodeOutput, String edgeOutput, int B, 
			ArrayList<String> filters, long latMin,	long latMax, long lonMin, long lonMax) 
			throws XMLStreamException, IOException {
		
		ObjectOutputStream outN = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(nodeOutput),B));
		ObjectOutputStream outE = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(edgeOutput),B));
		InputStream is = new FileInputStream(new File(input));
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLEventReader reader = xif.createXMLEventReader(is);
		long id = 0;
		float lon = 0, lat = 0;
		boolean insideWay = false;
		long lastWayNode = 0;
		String v = null;
		ArrayList<IncompleteEdge> edgeList = null; // List to store edges until we meet the highway tag
		while(reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if(event.getEventType() == XMLStreamConstants.START_ELEMENT) {
				StartElement start = event.asStartElement();
				String type = start.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					Iterator attributes = start.getAttributes();
					while(attributes.hasNext()) {
						Attribute attribute = (Attribute) attributes.next();
						if(attribute.getName().getLocalPart().equalsIgnoreCase("id")) {
							id = Long.parseLong(attribute.getValue());
						}
						else if(attribute.getName().getLocalPart().equalsIgnoreCase("lat")) {
							lat = Float.parseFloat(attribute.getValue());
						}
						else if(attribute.getName().getLocalPart().equalsIgnoreCase("lon")) {
							lon = Float.parseFloat(attribute.getValue());
						}						
					}
				}
				else if(type.equalsIgnoreCase("way")) {
					insideWay = true;
					lastWayNode = 0;
					edgeList = new ArrayList<IncompleteEdge>();
				}
				else if(type.equalsIgnoreCase("nd")) {
					if(insideWay) {
						long val = Long.parseLong(((Attribute)start.getAttributes().next()).getValue());
						if(lastWayNode == 0) {
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
								}
								else if(attribute.getValue().equalsIgnoreCase("oneway")) {
									for(int i = 0; i < edgeList.size(); i++) {
										edgeList.get(i).oneway = true;
									}
								}
							}
							else if(attribute.getName().getLocalPart().equalsIgnoreCase("v")) {
								v = attribute.getValue();
							}	
						}
					}
				}
			}
			else if(event.getEventType() == XMLStreamConstants.END_ELEMENT) {
				EndElement end = event.asEndElement();
				String type = end.getName().getLocalPart();
				if(type.equalsIgnoreCase("node")) {
					//System.out.println("Node: id="+id+" lat="+lat+" lon="+lon);
					if(latMin <= lat && lat <= latMax && lonMin <= lon && lon <= lonMax) {
						outN.writeObject(new IncompleteNode(id,lat,lon));
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
							}
						}
					}
					if(filter) {
						for(int i = 0; i < edgeList.size(); i++) {
							//IncompleteEdge edge = edgeList.get(i);
							//System.out.println("Edge from "+edge.nodeID1+ " to "+edge.nodeID2+" of type "+edge.type);
							outE.writeObject(edgeList.get(i));
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
