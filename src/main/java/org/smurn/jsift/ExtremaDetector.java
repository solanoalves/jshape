/*
 * Copyright 2011 Stefan C. Mueller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smurn.jsift;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math.linear.SingularMatrixException;

/**
 * Strategy that detects local extrema in a scale-space. The strategy searches
 * all pixels in the difference-of-gaussian images and returns the positions of
 * those who's value is either smaller or larger than all 26 neighboring pixels.
 * 
 * based on https://github.com/robwhess/opensift
 */
public class ExtremaDetector implements KeypointDetector {

	public static final LUSolver3 solver = new LUSolver3();
	private static final int SIFT_MAX_INTERP_STEPS = 5;
	private static final int SIFT_IMG_BORDER = 5;
	private static final int OFFSET = 0, PARTIAL_DERIVATIVES = 1, EDGE_RESPONSE = 2;
	private static final int SIFT_INTERVALS = 3;
	
	@Override
	public Collection<ScaleSpacePoint> detectKeypoints(final ScaleSpace scaleSpace) throws Exception {

		if (scaleSpace == null) {
			throw new NullPointerException("scale space must not be null");
		}

		List<ScaleSpacePoint> points = new ArrayList<ScaleSpacePoint>();
		
		int o = 0;
		for (Octave octave : scaleSpace.getOctaves()) {
			for (int i = 1; i < SIFT_INTERVALS; i++) {
				Collection<ScaleSpacePoint> pointsOnThisScale = detectKeypoints(
						o,
						octave.getDifferenceOfGaussians().get(i - 1), 
						octave.getDifferenceOfGaussians().get(i),
						octave.getDifferenceOfGaussians().get(i + 1),
						octave.getDifferenceOfGaussians(),
						i);
				points.addAll(pointsOnThisScale);
				
			}
			o++;
		}

		return points;
	}

	/**
	 * Detects extrema on one scale.
	 * 
	 * @param low
	 *            The DoG at one scale lower.
	 * @param center
	 *            The DoG on which to detect the extrema.
	 * @param high
	 *            The DoG at one scale higher.
	 * @return The extremas.
	 * @throws Exception 
	 */
	private Collection<ScaleSpacePoint> detectKeypoints(int octave, Image low, Image center, Image high, List<Image> dog, int dogCount) throws Exception {
		List<ScaleSpacePoint> points = new LinkedList<ScaleSpacePoint>();
		for (int row = SIFT_IMG_BORDER; row < center.getHeight() - SIFT_IMG_BORDER; row++) {
			colLoop: for (int col = SIFT_IMG_BORDER; col < center.getWidth() - SIFT_IMG_BORDER; col++) {
				float value = dog.get(dogCount).getPixel(row, col);
				boolean isExtremum = true;
				
				int k, l, m;
				if(value > 0) {
					maximum:
					for( k = -1; k <= 1; k++ )
						for( l = -1; l <= 1; l++ )
						  for( m = -1; m <= 1; m++ )
						    if( value < dog.get(dogCount+k).getPixel(row+l, col+m) ) {
						    	isExtremum = false;
						    	break maximum;
						    }
				}else {
					minimum:
					for( k = -1; k <= 1; k++ )
						for( l = -1; l <= 1; l++ )
						  for( m = -1; m <= 1; m++ )
						    if( value > dog.get(dogCount+k).getPixel(row+l, col+m) ) {
						    	isExtremum = false;
						    	break minimum;
						    }
				}

				if (isExtremum) {
					//github robwhess/opensift
					int i = 0;
					double[][] result = null;
					int inti = dogCount;
					int ri = row;
					int ci = col;
					while(i < SIFT_MAX_INTERP_STEPS) {
						try {
							result = interpolate(ri, ci, inti, dog);
						}catch(Exception e) {
							break colLoop;
						}
						if(Math.abs(result[OFFSET][0]) < 0.5 && Math.abs(result[OFFSET][1]) < 0.5 && Math.abs(result[OFFSET][2]) < 0.5)
							break;
						
						ci = (int)Math.round(ci+result[OFFSET][0]);
						ri = (int)Math.round(ri+result[OFFSET][1]);
						inti = (int)Math.round(inti+result[OFFSET][2]);						
						
						if(inti < 1 || inti > SIFT_INTERVALS || ri < SIFT_IMG_BORDER || ci < SIFT_IMG_BORDER 
								|| ri > dog.get(0).getHeight() - SIFT_IMG_BORDER || ci > dog.get(0).getWidth() - SIFT_IMG_BORDER)
							continue colLoop;
						
						i++;
					}
					
					if(i >= SIFT_MAX_INTERP_STEPS)
						continue colLoop;
					
					if(result == null)
						continue colLoop;
					
					//removing low contrast points
					double contrast = dog.get(inti).getPixel(ri, ci)+(result[PARTIAL_DERIVATIVES][0]*result[OFFSET][0] + result[PARTIAL_DERIVATIVES][1]*result[OFFSET][1] + result[PARTIAL_DERIVATIVES][2]*result[OFFSET][2])*0.5;
					if(Math.abs(contrast) < 0.07) {
						continue colLoop;
					}
					
					//remove edge responses
					if(result[EDGE_RESPONSE][0] <= 10.0 && result[EDGE_RESPONSE][1] > 0) {
						Point2D coords = center.toOriginal(new Point2D.Double(ci, ri));
						ScaleSpacePoint point = new ScaleSpacePoint(coords.getX(), coords.getY(), center.getSigma(), octave, inti);
						points.add(point);						
					}
				}
			}
		}
		return points;
	}
	
	public double[][] interpolate(int row, int col, int img, List<Image> dog) throws Exception {
		double partialX, partialY, partialS, offsetX, offsetY, offsetS;
		double[][] hessian = new double[3][3],
				invHessian;
		// Partial Derivatives vetor [dX, dY, dS]
		partialX = (dog.get(img).getPixel(row, col + 1) - dog.get(img).getPixel(row, col - 1)) / 2.0f;
		partialY = (dog.get(img).getPixel(row + 1, col) - dog.get(img).getPixel(row - 1, col)) / 2.0f;
		partialS = (dog.get(img+1).getPixel(row, col) - dog.get(img-1).getPixel(row, col)) / 2.0f;
		
		//Hessian
		hessian[0][0] = Fxx(row, col, dog.get(img));
		hessian[0][1] = Fxy(row, col, dog.get(img));
		hessian[0][2] = Fxs(row, col, dog.get(img+1), dog.get(img-1));
		hessian[1][0] = Fyx(row, col, dog.get(img));
		hessian[1][1] = Fyy(row, col, dog.get(img));
		hessian[1][2] = Fys(row, col, dog.get(img+1), dog.get(img-1));
		hessian[2][0] = Fsx(row, col, dog.get(img+1), dog.get(img-1));
		hessian[2][1] = Fsy(row, col, dog.get(img+1), dog.get(img-1));
		hessian[2][2] = Fss(row, col, dog.get(img+1), dog.get(img), dog.get(img-1));
		
		try {
			invHessian = solver.inverse(hessian);
		}catch(SingularMatrixException sme) {
			throw new Exception("Singular matrix");
		}
		
		//points h = -invH.(dDog/dX)T
		offsetX = -(invHessian[0][0]*partialX + invHessian[0][1]*partialY + invHessian[0][2]*partialS);
		offsetY = -(invHessian[1][0]*partialX + invHessian[1][1]*partialY + invHessian[1][2]*partialS);
		offsetS = -(invHessian[2][0]*partialX + invHessian[2][1]*partialY + invHessian[2][2]*partialS);
		
		//edge response < 10
		double edgeResponse = (Math.pow(hessian[0][0]+hessian[1][1], 2) / (hessian[0][0]*hessian[1][1]-Math.pow(hessian[0][1]*hessian[1][0],2)));
		double det = (hessian[0][0]*hessian[1][1]-Math.pow(hessian[0][1]*hessian[1][0],2));
		
		return new double[][] {{offsetX, offsetY, offsetS}, {partialX, partialY, partialS}, {edgeResponse, det, 0.0}};
	}

	// Difference Of Gaussian
	private double dog(int row, int col, Image image) {
		if(row < 0 || col < 0 || row == image.getHeight() || col == image.getWidth()) return 0.0;
		return image.getPixel(row, col);
	}

	// second derivative of Fx to x (https://en.wikipedia.org/wiki/Finite_difference)
	private double Fxx(int row, int col, Image image) {
		double v = dog(row, col, image);
		double fr = dog(row, col + 1, image);
		double fl = dog(row, col - 1, image);
		return fr + fl - 2*v;
	}
	// second derivative of Fy to y
	private double Fyy(int row, int col, Image image) {
		double v = dog(row, col, image);
		double fr = dog(row + 1, col, image);
		double fl = dog(row - 1, col, image);
		return fr + fl - 2*v;
	}
	// second derivative of Fs to s
	private double Fss(int row, int col, Image high, Image center, Image low) {
		double v = dog(row, col, center);
		double fl = dog(row, col, low);
		double fr = dog(row, col, high);
		return fr + fl - 2*v;
	}

	// second derivative of Fx to y
	private double Fxy(int row, int col, Image image) {
		double fr = dog(row+1, col+1, image) - dog(row-1, col+1, image);
		double fl = dog(row+1, col-1, image) + dog(row-1, col-1, image);
		return (fr - fl) / 4.0;
	}

	// second derivative of Fx to s
	private double Fxs(int row, int col, Image high, Image low) {
		double fr = dog(row, col+1, high) - dog(row, col+1, low);
		double fl = dog(row, col-1, high) + dog(row, col-1, low);
		return (fr - fl) / 4.0;
	}
	
	// second derivative of Fy to s
	private double Fys(int row, int col, Image high, Image low) {
		double fr = dog(row+1, col, high) - dog(row+1, col, low);
		double fl = dog(row-1, col, high) + dog(row-1, col, low);
		return (fr - fl) / 4.0;
	}
	
	// second derivative of Fy to x
	private double Fyx(int row, int col, Image image) {
		return Fxy(row, col, image);
	}
	
	// second derivative of Fs to x
	private double Fsx(int row, int col, Image high, Image low) {
		return Fxs(row, col, high, low);
	}
	
	// second derivative of Fs to y
	private double Fsy(int row, int col, Image high, Image low) {
		return Fys(row, col, high, low);
	}
	
	


}
