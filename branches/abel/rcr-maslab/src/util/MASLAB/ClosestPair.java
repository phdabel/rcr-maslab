package util.MASLAB;

import java.util.ArrayList;
import java.util.Arrays;


import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import util.MyPoint2D;

public class ClosestPair {

	private MyPoint2D best1, best2;
	private double bestDistance = Double.POSITIVE_INFINITY;
	private ArrayList<MyPoint2D> points1 = new ArrayList<MyPoint2D>();
	private ArrayList<MyPoint2D> points2 = new ArrayList<MyPoint2D>();
	
	public ClosestPair(Point2D[] points1, Point2D[] points2)
	{
		
		int N = points1.length + points2.length;
		if(N <= 1) return;
		
		
		MyPoint2D[] pointsByX = new MyPoint2D[N];
		for(int i = 0; i < points1.length; i++)
		{
			pointsByX[i] = new MyPoint2D(points1[i].getX(), points1[i].getY());
			this.points1.add(pointsByX[i]);
		}
		int auxN = points1.length;
		for(int i = 0; i < points2.length; i++){
			System.out.println("posicao "+(auxN+i));
			System.out.println("Ponto "+points2[i]);
			
			pointsByX[auxN+i] = new MyPoint2D(points2[i].getX(), points2[i].getY());
			this.points2.add(pointsByX[auxN+i]);
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
		
		MyPoint2D[] pointsByY = new MyPoint2D[N];
		for(int i = 0; i < N; i++) pointsByY[i] = pointsByX[i];
		
		MyPoint2D[] aux = new MyPoint2D[N];
		
		closest(pointsByX, pointsByY, aux, 0, (N-1));
		
	}
	
	public double closest(MyPoint2D[] pointsByX, MyPoint2D[] pointsByY, MyPoint2D[] aux, int lo, int hi)
	{
		if(hi <= lo) return Double.POSITIVE_INFINITY;
		int mid = lo + (hi - lo) / 2;
		
		MyPoint2D median = pointsByX[mid];
		
		double delta1 = closest(pointsByX, pointsByY, aux, lo, mid);
		double delta2 = closest(pointsByX, pointsByY, aux, mid+1, hi);
		double delta = Math.min(delta1, delta2);
		
		
		Merge.merge(pointsByY, aux, lo, mid, hi);
		
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
				if(this.pointsOnSameSegment(aux[i], aux[j]) == false){
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
		}
		
		return delta;
	}
	
	public MyPoint2D either() { 
		if(this.points1.contains(best1))
		{
			return best1;
		}else
		{
			return best2;
		}
	}
	public MyPoint2D other() {
		if(this.points2.contains(best2))
		{
			return best2;
		}else{
			return best1;
		}
		 
	}
	
	public double distance(){
		return this.bestDistance;
	}
	
	public Boolean pointsOnSameSegment(MyPoint2D p1, MyPoint2D p2)
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
