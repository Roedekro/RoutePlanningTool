package Address;

import java.util.ArrayList;

import Address.AdrNodeCollection.Pair;

public class K2Tree {
	
	public int height;
	public AdrNode median;
	public K2Tree leftSubtree;
	public K2Tree rightSubtree;
	public K2Tree parent;
	public boolean leaf = false;
	public Range range;
	
	public K2Tree(AdrNodeCollection col, int height, K2Tree parent, Range range) {
		
		this.parent = parent;
		this.range = range;
		
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
				leftSubtree = new K2Tree(pair.left, height+1, this, 
						new Range(range.latMin, median.lat, range.lonMin, range.lonMax));
				rightSubtree = new K2Tree(pair.right, height+1, this,
						new Range(median.lat, range.latMax, range.lonMin, range.lonMax));
			}
			else {
				median = col.lon.get(col.lon.size()/2);
				Pair pair = col.splitByLon(median.lon);
				leftSubtree = new K2Tree(pair.left, height+1, this,
						new Range(range.latMin, range.latMax, range.lonMin, median.lon));
				rightSubtree = new K2Tree(pair.right, height+1, this,
						new Range(range.latMin, median.lat, median.lon, range.lonMax));
			}
		}
	}
	
	public ArrayList<AdrNode> query(Range range) {
		
		ArrayList<AdrNode> ret = new ArrayList<AdrNode>();
		if(leaf) {
			if(range.contains(median)) {
				ret.add(median);
			}
		}
		else {
			if(height %2 == 1) {
				if(range.intersects(leftSubtree.range)) {
					ret.addAll(leftSubtree.query(range));
				}
				if(range.intersects(rightSubtree.range)) {
					ret.addAll(rightSubtree.query(range));
				}
			}
			else {
				if(range.intersects(leftSubtree.range)) {
					ret.addAll(leftSubtree.query(range));
				}
				if(range.intersects(rightSubtree.range)) {
					ret.addAll(rightSubtree.query(range));
				}
			}
		}	
		return ret;
	}
}
