package util.MASLAB;

import java.util.Comparator;
import util.MyPoint2D;

public class Point2DXSorter implements Comparator<MyPoint2D> {

	@Override
	public int compare(MyPoint2D p1, MyPoint2D p2)
	{
		return (p1.getX() > p2.getX() ? -1 : (p1.getX() == p2.getX() ? 0 : 1));
	}
	
}