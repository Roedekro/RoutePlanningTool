package Address;

import java.util.ArrayList;

import Address.AdrNodeCollection.Pair;

public class K2Tree {
	
	public int height;
	public AdrNode median;
	public K2Tree leftSubtree = null;
	public K2Tree rightSubtree = null;
	public K2Tree parent;
	public boolean leaf = false;
	public Range range;
	
	public K2Tree(AdrNodeCollection col, int height, K2Tree parent, Range range) {
		
		this.parent = parent;
		this.range = range;
		
		//System.out.println(col.lat.size());
		
		if(col.lat.size() == 1) {
			median = col.lat.get(0);
			leaf = true;
			// Done!
		}
		else {
			// Split elements either by lat or lon
			
			if(height %2 == 1) {
				median = col.lat.get(col.lat.size()/2);
				Pair pair = col.splitByLat(median.lat);
				if(pair.left.lat.size() != 0) {
					leftSubtree = new K2Tree(pair.left, height+1, this, 
						new Range(range.latMin, median.lat, range.lonMin, range.lonMax));
				}
				if(pair.right.lat.size() != 0) {
					rightSubtree = new K2Tree(pair.right, height+1, this,
						new Range(median.lat, range.latMax, range.lonMin, range.lonMax));
				}
			}
			else {
				median = col.lon.get(col.lon.size()/2);
				Pair pair = col.splitByLon(median.lon);
				if(pair.left.lat.size() != 0) {
					leftSubtree = new K2Tree(pair.left, height+1, this,
						new Range(range.latMin, range.latMax, range.lonMin, median.lon));
				}
				if(pair.right.lat.size() != 0) {
					rightSubtree = new K2Tree(pair.right, height+1, this,
						new Range(range.latMin, range.latMax, median.lon, range.lonMax));
				}
			}
		}
	}
	
	public ArrayList<AdrNode> query(Range query) {
		
		/*System.out.println(Double.toString(range.latMin) + " " + Double.toString(range.latMax) + " " +
				Double.toString(range.lonMin) + " " + Double.toString(range.lonMax));
		System.out.println(leaf + " " + leftSubtree + " " + rightSubtree);
		if(leftSubtree != null) {
			System.out.println("Left " + query.intersects(leftSubtree.range));
			System.out.println(Double.toString(leftSubtree.range.latMin) + " " + 
					Double.toString(leftSubtree.range.latMax) + " " +
					Double.toString(leftSubtree.range.lonMin) + " " + 
					Double.toString(leftSubtree.range.lonMax));
		}
		if(rightSubtree != null) {
			System.out.println("Right " + (rightSubtree.range.intersects(query)));
			System.out.println(rightSubtree.range.latMin + " " + rightSubtree.range.latMax + " " +
					rightSubtree.range.lonMin + " " + rightSubtree.range.lonMax);
		}*/
		
		ArrayList<AdrNode> ret = new ArrayList<AdrNode>();
		if(leaf) {
			if(query.contains(median)) {
				ret.add(median);
			}
		}
		else {
			if(leftSubtree!=null && (leftSubtree.range.intersects(query))) {
			//if(leftSubtree!=null && (query.intersects(leftSubtree.range))) {
				ret.addAll(leftSubtree.query(query));
			}
			else if(leftSubtree!=null && query.contains(leftSubtree.range)) {
				ret.addAll(leftSubtree.report());
			}
			if(rightSubtree!=null && (rightSubtree.range.intersects(query))) {
				ret.addAll(rightSubtree.query(query));
			}
			else if(rightSubtree!=null && query.contains(rightSubtree.range)) {
				ret.addAll(rightSubtree.report());
			}
		}	
		return ret;
	}
	
	public ArrayList<AdrNode> report() {
		ArrayList<AdrNode> ret = new ArrayList<AdrNode>();
		if(leaf) {
			ret.add(median);
		}
		else {
			if(leftSubtree != null) {
				ret.addAll(leftSubtree.report());
			}
			if(rightSubtree != null) {
				ret.addAll(rightSubtree.report());
			}
		}
		return ret;
	}
}
