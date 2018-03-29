package br.sln.jshape;

public class KnnPoint {
	private int x;
	private int y;
	private boolean outlier;
	public KnnPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public boolean isOutlier() {
		return outlier;
	}
	public void setOutlier(boolean outlier) {
		this.outlier = outlier;
	}
	public double distance(KnnPoint point) {
		if(point != null) {
			return Math.sqrt( Math.pow(this.x - point.getX(), 2) + Math.pow(this.y - point.getY(), 2) );
		}
		return Double.MAX_VALUE;
	}
}
