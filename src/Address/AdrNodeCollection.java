package Address;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AdrNodeCollection {

	public ArrayList<AdrNode> lat;
	public ArrayList<AdrNode> lon;
	
	public AdrNodeCollection(ArrayList<AdrNode> array) {
		
		lat = new ArrayList<AdrNode>();
		lat.addAll(array);
		Collections.sort(lat, new Comparator<AdrNode>() {
			@Override
			public int compare(AdrNode n1, AdrNode n2) {
				return Double.compare(n1.lat, n2.lat);
			}
		});
		
		lon = new ArrayList<AdrNode>();
		lon.addAll(array);
		Collections.sort(lon, new Comparator<AdrNode>() {
			@Override
			public int compare(AdrNode n1, AdrNode n2) {
				return Double.compare(n1.lon, n2.lon);
			}
		});
	}
	
	public AdrNodeCollection(ArrayList<AdrNode> lat, ArrayList<AdrNode> lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public Pair splitByLat(double split) {
		
		ArrayList<AdrNode> leftLat = new ArrayList<AdrNode>();
		ArrayList<AdrNode> rightLat = new ArrayList<AdrNode>();
		
		for(int i = 0; i < lat.size(); i++) {
			if(lat.get(i).lat < split) {
				leftLat.add(lat.get(i));
			}
			else {
				rightLat.add(lat.get(i));
			}
		}
		
		ArrayList<AdrNode> leftLon = new ArrayList<AdrNode>();
		ArrayList<AdrNode> rightLon = new ArrayList<AdrNode>();
		
		for(int i = 0; i < lon.size(); i++) {
			if(lon.get(i).lat < split) {
				leftLon.add(lon.get(i));
			}
			else {
				rightLon.add(lon.get(i));
			}
		}
		
		return new Pair(new AdrNodeCollection(leftLat,leftLon),
				new AdrNodeCollection(rightLat,rightLon));
		
	}
	
public Pair splitByLon(double split) {
		
		ArrayList<AdrNode> leftLat = new ArrayList<AdrNode>();
		ArrayList<AdrNode> rightLat = new ArrayList<AdrNode>();
		
		for(int i = 0; i < lat.size(); i++) {
			if(lat.get(i).lon < split) {
				leftLat.add(lat.get(i));
			}
			else {
				rightLat.add(lat.get(i));
			}
		}
		
		ArrayList<AdrNode> leftLon = new ArrayList<AdrNode>();
		ArrayList<AdrNode> rightLon = new ArrayList<AdrNode>();
		
		for(int i = 0; i < lon.size(); i++) {
			if(lon.get(i).lon < split) {
				leftLon.add(lon.get(i));
			}
			else {
				rightLon.add(lon.get(i));
			}
		}
		
		return new Pair(new AdrNodeCollection(leftLat,leftLon),
				new AdrNodeCollection(rightLat,rightLon));
		
	}
	
	 public static class Pair {

        public AdrNodeCollection left;
        public AdrNodeCollection right;

        public Pair(AdrNodeCollection left, AdrNodeCollection right) {
            this.left = left;
            this.right = right;
        }
    }
}
