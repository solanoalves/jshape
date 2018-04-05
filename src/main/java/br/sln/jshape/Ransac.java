package br.sln.jshape;

import java.awt.Graphics;

/**
 * @author Constantin Berzan
 */

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * RANSAC line-detection algorithm. All parameters are public fields.
 *
 * Modifications to avoid overfitting to "dense areas" (in a hallway, have many
 * more points to our sides than we do in front). - If R2 from best-fit line is
 * bad, store the original line (computed based on a sample, and with enough
 * consenting points) rather than the new (overfitted) least-squares line. - Run
 * the greedy RANSAC algorithm repeatedly, and choose a result that matches
 * enough points in less than the maximum number of iterations. - Additionally
 * in the step above, don't select a result if it has many confounded points
 * (points nearby more than one line).
 *
 * Ideas for further improvement: - If we run 10 times now and we're in a bad
 * spot, we stupidly return the last result. Instead of rejecting results based
 * on thresholds, score results, and select the best.
 */
public class Ransac {
	/// Iterations of the greedy line-finder.
	public int metaIterations = 10;
	/// Iterations random sampling to find one line.
	public int iterations = 100;
	/// Size of initial sample.
	public int sampleSize = 5;
	/// Width of laser beam to consider for initial sample.
	public int sampleBeamWidth = 10;
	/// Max distance of a point that still counts as being on the line.
	public double maxBelongDist = 10.0;
	/// Min number of points on a line to count it as valid.
	public int minConsensus = 30;
	/// R2 threshold under which a best-fit-line is bad.
	public double badR2threshold = 0.95;
	/// How many points can be confounded (fit multiple lines) for the result to
	/// still be valid.
	public int confoundThreshold = 15;

	public Ransac() {
	}

	/// Repeatedly runs RANSAC until it finds a good-enough solution.
	public Line[] findLines(List<KnnPoint> points) {
		long timer = System.currentTimeMillis();
		ArrayList<Line> lines = new ArrayList<Line>();
		Point2D.Double[] points2D = new Point2D.Double[points.size()];

		int metaIter;
		for (metaIter = 0; metaIter < metaIterations; metaIter++) {
			lines.clear();
			int iter = findLinesGreedy(points, lines, points2D);
			int confounds = countConfounds(points, lines, points2D);
			// System.out.format("metaIter=%d found %d lines (%d confounds) in %d greedy
			// iterations\n",
			// metaIter, lines.size(), confounds, iter);
			if (iter < iterations && confounds < confoundThreshold) {
				// This is a good catch because:
				// - it ran for few iterations, therefore many points were matched
				// - there are few confounds, therefore there are no "duplicate" lines
				metaIter++; // for stats only
				break;
			}
		}
		// If we don't break early, we just return the lines from the last attempt.

		timer = System.currentTimeMillis() - timer;
		// System.out.format("RANSAC ran %d meta-iterations (%d total iterations) " +
		// "in %.3f seconds, found %d lines.\n",
		// metaIter, totalIter, timer / 1000.0, lines.size());
		return lines.toArray(new Line[0]); // I can't believe Java requires the type like this.
	}

	/**
	 * Finds lines in the given laser readings, using RANSAC. Fills in the lines
	 * array, and returns the number of iterations ran.
	 */
	public int findLinesGreedy(List<KnnPoint> _points, ArrayList<Line> lines, Point2D.Double[] points) {
		knn2Point2D(_points, points);

		int iter;
		for (iter = 0; iter < iterations; iter++) {
			// System.out.format("Iteration %d\n", iter);
			// Quit early if out of points.
			if (points.length < minConsensus)
				break;
			// TODO: also break early if we run X times in a row unsuccessfully?

			// Get an initial sample.
			Point2D.Double[] sample = getInitialSample(points);

			// Compute best-fit line.
			Line bestFit = new Line();
			bestFitLine(sample, bestFit);

			// See how many points fit this line well.
			ArrayList<Integer> consenting = new ArrayList<Integer>();
			for (int i = 0; i < points.length; i++) {
				if (bestFit.distance(points[i]) < maxBelongDist)
					consenting.add(i);
			}

			// If there is consensus, compute new best-fit line of consenting
			// points, store it, and remove consenting points from further
			// consideration.
			if (consenting.size() >= minConsensus) {
				Point2D.Double[] selected = new Point2D.Double[consenting.size()];
				for (int i = 0; i < consenting.size(); i++)
					selected[i] = points[consenting.get(i)];
				Line newBestFit = new Line();
				double r2 = bestFitLine(selected, newBestFit);
				// System.out.format("iter=%d Consensus r2 = %f\n", iter, r2);
				if (r2 < badR2threshold) {
					// Go out on a limb and save the original line, rather than
					// the new "best fit" line. This is a HACK against the new
					// line fitting points with high density too well...
					lines.add(bestFit);
				} else {
					lines.add(newBestFit);
				}
				points = removeIndices(points, consenting);
			} else {
				// Otherwise discard the line and try again.
				bestFit = null;
			}

		}
		return iter;
	}

	private Point2D.Double[] knn2Point2D(List<KnnPoint> points, Point2D.Double[] _points) {
		for (int i = 0; i < points.size(); i++) {
			_points[i] = new Point2D.Double(points.get(i).getX(), points.get(i).getY());
		}
		return _points;
	}

	/// Get an initial sample out of a set of points sorted by angle.
	private Point2D.Double[] getInitialSample(Point2D.Double[] points) {
		Random rand = new Random();
		assert (sampleBeamWidth <= points.length);
		int beamBegin = rand.nextInt(points.length - sampleBeamWidth + 1);
		ArrayList<Integer> indices = new ArrayList<Integer>(sampleBeamWidth);
		for (int i = 0; i < sampleBeamWidth; i++)
			indices.add(i, beamBegin + i);
		Collections.shuffle(indices);
		Point2D.Double[] sample = new Point2D.Double[sampleSize];
		for (int i = 0; i < sampleSize; i++)
			sample[i] = points[indices.get(i)];
		return sample;
	}

	/**
	 * Returns copy of array, with specified indices removed.
	 */
	private Point2D.Double[] removeIndices(Point2D.Double[] points, ArrayList<Integer> indices) {
		// Create new point list, removing matched points.
		Point2D.Double[] copy = new Point2D.Double[points.length];
		for (int i = 0; i < points.length; i++)
			copy[i] = points[i];
		for (int index : indices)
			copy[index] = null;
		Point2D.Double[] newPoints = new Point2D.Double[points.length - indices.size()];
		int pos = 0;
		for (Point2D.Double p : copy) {
			if (p != null) {
				assert (pos < newPoints.length);
				newPoints[pos] = p;
				pos++;
			}
		}
		return newPoints;
	}

	/**
	 * Finds the least-squares best-fit line for a given set of points. According to
	 * http://mathworld.wolfram.com/LeastSquaresFitting.html (formulae 16 and on).
	 *
	 * Uses a trick to handle vertical lines: it just swaps the x and y coordinates
	 * to turn it into a horizontal line and avoid division by near-zero.
	 */
	double bestFitLine(Point2D.Double[] points, Line line) {
		int n = points.length;

		double ss_xx = 0, ss_yy = 0, ss_xy = 0, mean_x = 0, mean_y = 0, sigX = 0, sigY = 0, covXY = 0, a = 0, b = 0;
		for (Point2D.Double p : points) {
			ss_xx += p.x * p.x;
			ss_yy += p.y * p.y;
			ss_xy += p.x * p.y;
			mean_x += p.x;
			mean_y += p.y;
		}
		mean_x /= n;
		mean_y /= n;
		ss_xx -= n * mean_x * mean_x;
		ss_yy -= n * mean_y * mean_y;
		ss_xy -= n * mean_x * mean_y;
		
		sigX = ss_xx / n;
		sigY = ss_yy / n;
		covXY = ss_xy / n;
		
		//regression coeficient
		b = ss_xy / ss_xx;
		a = mean_y - b*mean_x; 
		double r2 = ss_xy * ss_xy / (ss_xx * ss_yy);
		return r2;
	}

	/*
	 * Returns number of points that are within maxBelongDist of more than one line.
	 */
	int countConfounds(List<KnnPoint> _points, ArrayList<Line> lines, Point2D.Double[] points) {
		int count = 0;
		for (Point2D.Double p : points) {
			int claimers = 0;
			for (Line l : lines) {
				if (l.distance(p) < maxBelongDist)
					claimers++;
			}
			if (claimers > 1)
				count++;
		}
		return count;
	}

////	public static void drawLine(Line line, Graphics g) {
//		// Take two points on the line, w.r.t. robot origin.
////		Point2D.Double ar = new Point2D.Double(), br = new Point2D.Double();
////		double slope = -line.a / line.b;
////		if (slope >= -1 && slope <= 1) {
//			// Line closer to horizontal.
////			double m = -line.a / line.b, b = -line.c / line.b;
//			ar.x = 0;
//			ar.y = m * ar.x + b;
//			br.x = 1;
//			br.y = m * br.x + b;
//		} else {
//			// Line closer to vertical.
//			double rm = -line.b / line.a, rb = -line.c / line.a;
//			ar.y = 0;
//			ar.x = rm * ar.y + rb;
//			br.y = 1;
//			br.x = rm * br.y + rb;
//		}
//
//		g.drawLine(x1, y1, x2, y2);
//		// // Convert the points to world coordinates.
//		// Point2D.Double aw = robot.robot2world(robotPose, ar),
//		// bw = robot.robot2world(robotPose, br);
//		// // Compute projection of (0, 0) onto line through aw and bw.
//		// double abx = bw.x - aw.x,
//		// aby = bw.y - aw.y,
//		// acx = 0 - aw.x,
//		// acy = 0 - aw.y;
//		// double ab2 = abx * abx + aby * aby;
//		// double r = (abx * acx + aby * acy) / ab2;
//		// Point2D.Double result = new Point2D.Double();
//		// result.x = aw.x + r * (bw.x - aw.x);
//		// result.y = aw.y + r * (bw.y - aw.y);
//		// return result;
////	}
};
