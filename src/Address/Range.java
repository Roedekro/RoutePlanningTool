package Address;

public class Range {
		
	public double latMin;
	public double latMax;
	public double lonMin;
	public double lonMax;
	
	public Range(double latMin, double latMax, double lonMin, double lonMax) {
		
		this.latMin = latMin;
		this.latMax = latMax;
		this.lonMin = lonMin;
		this.lonMax = lonMax;
	}
	
	/*
	 * Contains range
	 */
	public boolean contains(Range range) {
        return (latMin <= range.latMin &&
        		latMax >= range.latMax &&
				lonMin <= range.lonMin &&
				lonMax >= range.lonMax);
    }

	/*
	 * Is the given node contained in this range
	 */
    public boolean contains(AdrNode node) {
        return (latMin <= node.lat && latMax >= node.lat &&
                lonMin <= node.lon && lonMax >= node.lon);
    }

    /*
     * Does the given range intersect this range
     */
    public boolean intersects(Range range) {
    	return (((latMin <= range.latMin && range.latMin <= latMax) || 
    			(latMin <= range.latMax && range.latMax <= latMax)) || 
    			((lonMin <= range.lonMin && range.lonMin <= lonMax) || 
    			(lonMin <= range.lonMax && range.lonMax <= lonMax)));
    }
	
    public void printRange() {
    	System.out.println(Double.toString(latMin) + " " + Double.toString(latMax) + " " +
				Double.toString(lonMin) + " " + Double.toString(lonMax));
    }
	
}
