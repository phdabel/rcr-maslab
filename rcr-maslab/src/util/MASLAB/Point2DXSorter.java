package util.MASLAB;

import java.util.Comparator;
import rescuecore2.misc.geometry.Point2D;

public class Point2DXSorter implements Comparator<Point2D> {

	@Override
	public int compare(Point2D p1, Point2D p2)
	{
		return (p1.getX() > p2.getX() ? -1 : (p1.getX() == p2.getX() ? 0 : 1));
	}
	
}