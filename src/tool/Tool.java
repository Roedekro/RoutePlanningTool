package tool;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.imageio.ImageIO;

/**
 * Tool for reading in Node files.
 * @author Martin
 *
 */
public class Tool {

	private int B = 8192;
	private ObjectInputStream in = null;
	@SuppressWarnings("unused")
	private String file = null;
	private boolean open = false;
	
	/**
	 * Reads in a file of Nodes and returns them as an ArrayList.
	 * Does not require a call to Open() or Close().
	 * @param input File of Nodes
	 * @return ArrayList<Node>
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ArrayList<Node> getNodesAsArrayList(String input) throws FileNotFoundException, IOException {
		
		ArrayList<Node> ret = new ArrayList<Node>();
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input),B));
		Node node = null;
		while(true) {
			try {
				node = (Node) oin.readUnshared();
				//oin.reset();
				ret.add(node);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// EOF
				break;
			}
		}
		oin.close();
	
		return ret;
	}
	
	/**
	 * Reads in a file of Nodes and returns them as a LinkedList.
	 * Does not require a call to Open() or close().
	 * @param input File of Nodes
	 * @return LinkedList<Node>
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public LinkedList<Node> getNodesAsLinkedList(String input) throws FileNotFoundException, IOException {
		
		LinkedList<Node> ret = new LinkedList<Node>();
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input),B));
		Node node = null;
		while(true) {
			try {
				node = (Node) oin.readUnshared();
				//oin.reset();
				ret.add(node);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// EOF
				break;
			}
		}
		oin.close();
	
		return ret;
	}
	
	/**
	 * PRIVATE because getSingleNode doesnt work.
	 * Opens an ObjectInputStream to the given file.
	 * @param file
	 */
	@SuppressWarnings("unused")
	private void open(String file) {
		this.file = file;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file),B));
			open = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * PRIVATE because getSingleNode doesnt work.
	 * Closes the ObjectInputStream.
	 */
	@SuppressWarnings("unused")
	private void close() {
		if(open) {
			try {
				in.close();
				open = false;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * DOESNT WORK. For some reason it will only read the first object repeatedly.
	 * If a file has been opened it will return a Node from the file.
	 * When End Of File is reached it will return null.
	 * If no file is open it will likewise return null.
	 * @return
	 */
	@SuppressWarnings("unused")
	private Node getSingleNode() {
		Node ret = null;
		if(open) {
			try {
				ret = (Node) in.readUnshared();
				//in.reset();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				ret = null;
			}
		}
		return ret;
	}
	
	/**
	 * Creates a white grey map based on input.
	 * @param input .txt file containing nodeID, latitude and longitude each line, separated by whitespace. Final line reads "end".
	 * @param output name of output file.
	 * @param height pixels.
	 * @param width pixels.
	 * @throws IOException 
	 */
	public void createAndFillImage(String input, String output, int height, int width, double minLat, 
			double maxLat,	double minLon, double maxLon) throws IOException {
		
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		BufferedReader in = new BufferedReader(new FileReader(input));
		String s = " ";
		String[] split = null;
		double latTotal = maxLat - minLat;
		double lonTotal = maxLon - minLon;
		double lat, lon, tempLat, tempLon;
		int x, y;
		while(true) {
			s = in.readLine();
			if(s.equalsIgnoreCase("end")) {
				break;
			}
			split = s.split(" ");
			tempLat = Double.parseDouble(split[1]);
			tempLon = Double.parseDouble(split[2]);
			lat = tempLat - minLat;
			lon = tempLon - minLon;
			x = (int) (width * (lon/lonTotal)) - 1;
			y = (int) (height * (lat/latTotal)) - 1;
			//System.out.println(x+" "+y);
			if(x < 0) {
				x = 0;
			}
			if(y < 0) {
				y = 0;
			}
			y = height - 1 - y;
			//System.out.println(x + " " + y);
			img.setRGB(x, y, Color.GRAY.getRGB());
		}
		in.close();
		ImageIO.write(img,"png",new File(output));
		System.out.println("Created white/grey image.");
	}
	
	public void drawOnImage(String inputImage, String inputNodes, String output, int height, int width, double minLat, 
			double maxLat,	double minLon, double maxLon, Color color) throws IOException {
		
		BufferedImage img = ImageIO.read(new File(inputImage));
		BufferedReader in = new BufferedReader(new FileReader(inputNodes));
		String s = " ";
		String[] split = null;
		double latTotal = maxLat - minLat;
		double lonTotal = maxLon - minLon;
		double lat, lon, tempLat, tempLon;
		int x, y;
		while(true) {
			s = in.readLine();
			if(s.equalsIgnoreCase("end")) {
				break;
			}
			split = s.split(" ");
			tempLat = Double.parseDouble(split[1]);
			tempLon = Double.parseDouble(split[2]);
			lat = tempLat - minLat;
			lon = tempLon - minLon;
			x = (int) (width * (lon/lonTotal)) - 1;
			y = (int) (height * (lat/latTotal)) - 1;
			if(x < 0) {
				x = 0;
			}
			if(y < 0) {
				y = 0;
			}
			y = height - y;
			img.setRGB(x, y, color.getRGB());
			if(color == Color.GREEN) {
				int i = y-5;
				if(i < 0) {
					i = 0;
				}
				int toI = y+6;
				if(toI > height-1) {
					toI = height-1;
				}
				int j2 = x-5;
				if(j2 < 0) {
					j2 = 0;
				}
				int toJ = x+6;
				if(toJ > width-1) {
					toJ = width-1;
				}
				while(i < toI) {
					int j = j2;
					while(j < toJ) {
						img.setRGB(j, i, color.getRGB());
						j++;
					}
					
					i++;
				}
			}
		}
		in.close();
		ImageIO.write(img,"png",new File(output));
		System.out.println("Finished drawing on image.");
	}
}
