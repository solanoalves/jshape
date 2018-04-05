package br.sln.jshape;


import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * @author Constantin Berzan
 * Holds a 2D line: ax + by + c = 0.
 * 
 * https://github.com/cberzan
 */
public class Line implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public double a;
    public double b;
    public double c;

    /**
     * Point-to-line distance.
     * See http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
     */
    public double distance(Point2D.Double point) {
        return Math.abs(a * point.x + b * point.y + c) / Math.sqrt(a * a + b * b);
    }
}
