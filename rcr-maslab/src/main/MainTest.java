package main;

import rescuecore2.misc.geometry.Point2D;
import util.MASLAB.ClosestPair;

public class MainTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Point2D a1 = new Point2D(1, 1);
		Point2D a2 = new Point2D(1, 2);
		Point2D a3 = new Point2D(1, 3);
		Point2D a4 = new Point2D(2, 1);
		Point2D a5 = new Point2D(2, 3);
		Point2D a6 = new Point2D(3, 1);
		Point2D a7 = new Point2D(3, 2);
		Point2D a8 = new Point2D(3, 3);
		
		Point2D b4 = new Point2D(5, 1);
		Point2D b5 = new Point2D(5, 3);
		Point2D b6 = new Point2D(6, 1);
		Point2D b7 = new Point2D(6, 3);
		Point2D b8 = new Point2D(7, 1);
		Point2D b9 = new Point2D(7, 2);
		Point2D b10 = new Point2D(7, 3);
		
		Point2D[] area1 = {a1, a2, a3, a4, a5, a6, a7, a8};
		Point2D[] area2 = { b4, b5, b6, b7, b8, b9, b10};
		
		ClosestPair c = new ClosestPair(area1, area2);
		
		System.out.println(c.either());
		System.out.println(c.other());
		

	}

}
