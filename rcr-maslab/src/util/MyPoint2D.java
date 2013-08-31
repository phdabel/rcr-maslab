package util;

import rescuecore2.misc.geometry.Point2D;

public class MyPoint2D extends Point2D implements Comparable<Point2D> {

	public MyPoint2D(double x, double y) {
		super(x, y);
	}

	@Override
	public int compareTo(Point2D o) {
		return (this.getX() > o.getX() ? -1 : (this.getX() == o.getX() ? 0 : 1));
	}
	
	

}
