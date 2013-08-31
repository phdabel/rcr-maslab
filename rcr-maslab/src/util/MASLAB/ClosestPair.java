package util.MASLAB;

import java.util.ArrayList;
import java.util.Arrays;


import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import util.MyPoint2D;

public class ClosestPair {

	private Point2D best1, best2;
	private double bestDistance = Double.POSITIVE_INFINITY;
	private ArrayList<Point2D> points1 = new ArrayList<Point2D>();
	private ArrayList<Point2D> points2 = new ArrayList<Point2D>();
	
	public ClosestPair(Point2D[] points1, Point2D[] points2)
	{
		
		int N = points1.length + points2.length;
		if(N <= 1) return;
		
		Point2D[] pointsByX = new Point2D[N];
		for(int i = 0; i < points1.length; i++)
		{
			pointsByX[i] = points1[i];
			this.points1.add(points1[i]);
		}
		int auxN = pointsByX.length;
		for(int i = 0; i < points2.length; i++){
			pointsByX[auxN + i] = points2[i];
			this.points2.add(points2[i]);
		}
		
		Arrays.sort(pointsByX, new Point2DXSorter());
		
		for(int i = 0; i < N-1; i++){
			if(pointsByX[i].equals(pointsByX[i+1]) && this.pointsOnSameSegment(pointsByX[i], pointsByX[i+1]) == false ){
				bestDistance = 0.0;
				best1 = pointsByX[i];
				best2 = pointsByX[i+1];
				return;
			}
		}
		
		Point2D[] pointsByY = new Point2D[N];
		for(int i = 0; i < N; i++) pointsByY[i] = pointsByX[i];
		
		Point2D[] aux = new Point2D[N];
		
		closest(pointsByX, pointsByY, aux, 0, N-1);
		
	}
	
	public double closest(Point2D[] pointsByX, Point2D[] pointsByY, Point2D[] aux, int lo, int hi)
	{
		if(hi <= lo) return Double.POSITIVE_INFINITY;
		int mid = lo + (hi - lo) / 2;
		
		Point2D median = pointsByX[mid];
		
		double delta1 = closest(pointsByX, pointsByY, aux, lo, mid);
		double delta2 = closest(pointsByX, pointsByY, aux, mid+1, hi);
		double delta = Math.min(delta1, delta2);
		
		Merge.merge((MyPoint2D[])pointsByY, (MyPoint2D[])aux, lo, mid, hi);
		
		int M = 0;
		
		for(int i = 0; i <= hi; i++)
		{
			if( (Math.abs(pointsByY[i].getX() - median.getX()) < delta) )
			{
				aux[M++] = pointsByY[i];
			}
		}
		
		for(int i = 0; i < M; i++)
		{
			for(int j = i+1; (j<M) && (aux[j].getY() - aux[i].getY() < delta); j++)
			{
				double distance = GeometryTools2D.getDistance(aux[i], aux[j]);
				if(distance < delta){
					delta = distance;
					if(distance < this.bestDistance)
					{
						this.bestDistance = delta;
						this.best1 = aux[i];
						this.best2 = aux[j];
					}
				}
			}
		}
		
		return delta;
	}
	
	public Point2D either() { return best1; }
	public Point2D other() { return best2; }
	
	public double distance(){
		return this.bestDistance;
	}
	
	public Boolean pointsOnSameSegment(Point2D p1, Point2D p2)
	{
		if(
				(this.points1.contains(p1) && this.points1.contains(p2))
				||
				(this.points2.contains(p1) && this.points2.contains(p2))
		   )
		{
			return true;
		}else{
			return false;
		}
	}
	
}
