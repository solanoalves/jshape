package br.sln.jshape;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class TransformImage {
	
	public static BufferedImage filterBottomImage(BufferedImage base) {
		BufferedImage bi = new BufferedImage(base.getWidth(), base.getHeight(), base.getType());
		Graphics2D g = bi.createGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, bi.getWidth(), bi.getHeight());
		g.drawImage(base.getSubimage(0, 2*base.getHeight()/3, base.getWidth(), base.getHeight()/3), 0, 2*base.getHeight()/3, null);
		return bi;
	}
	
	public static List<KnnPoint> blobPoints(BufferedImage image){
		List<KnnPoint> pontos = new ArrayList<>();
		PriorityQueue<KnnPoint> queue = new PriorityQueue<KnnPoint>(4, new KnnPointComparator());

		if (image != null) {
			int sum;
			for (int r = 0; r < image.getHeight(); r++) {
				loop: for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRGB(c, r) == Color.BLACK.getRGB()) {
						sum = 0;
						for (int i = -2; i <= 2; i++)
							for (int j = -2; j <= 2; j++)
								if (c + i > 0 && r + j > 0 && c + i < image.getWidth() && r + j < image.getHeight()) {
									sum += (image.getRGB(c + i, r + j) == Color.BLACK.getRGB() ? 1 : 0);
								}
						if (sum > 10) {
							KnnPoint newp = new KnnPoint(c, r);
							for(KnnPoint kp : queue) {
								if(kp.distance(newp) < 50) {
									break loop;
								}
							}
							queue.add(newp);
						}
					}
				}
			}
			while(!queue.isEmpty())
				pontos.add(queue.remove());
		}
		return pontos;
	}

	public static BufferedImage filterBlob(BufferedImage image) {
		BufferedImage blobs = null;
		if (image != null) {
			blobs = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
			int kernel = 2, k=2;
			int sum;
			boolean tl, tr, bl, br;
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRGB(c, r) == Color.BLACK.getRGB()) {
						sum = 0;
						tl=false; tr=false; bl=false; br=false;
						for (int i = -kernel; i <= kernel; i++)
							for (int j = -kernel; j <= kernel; j++)
								if (c + i > 0 && r + j > 0 && c + i < image.getWidth() && r + j < image.getHeight()) {
									if(image.getRGB(c + i, r + j) == Color.BLACK.getRGB()) {
										sum++;
										if(i < 0 && j < 0) tl = true;
										else if(i < 0 && j > 0) bl = true;
										else if(i > 0 && j < 0) tr = true;
										else br = true;
									}
								}
						if (sum > 20 && tl && tr && bl && br) {
							for (int i = -kernel*k; i <= kernel*k; i++)
								for (int j = -kernel*k; j <= kernel*k; j++)
									if (c + i > 0 && r + j > 0 && c + i < image.getWidth() && r + j < image.getHeight())
										blobs.setRGB(c+i, r+j, Color.BLACK.getRGB());
						}else {
							blobs.setRGB(c, r, Color.WHITE.getRGB());	
						}
					}else {
						blobs.setRGB(c, r, Color.WHITE.getRGB());
					}
				}
			}
		}
		return blobs;
	}
	
	public static BufferedImage minCount(BufferedImage image) {
		BufferedImage base = null;
		if (image != null) {
			int window = 10;
			int sum;
			base = deepCopy(image);
			Graphics g = base.getGraphics();
			g.setColor(new Color(0, 0, 0));
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if(image.getRGB(c, r) == Color.BLACK.getRGB()) {
						sum = 0;
						janela: for (int i = -window; i < window; i++) {
							for (int j = -window; j < window; j++) {
								if (c + i > 0 && r + j > 0 && c + i < image.getWidth() && r + j < image.getHeight()) {
									sum += (image.getRGB(c + i, r + j) == Color.BLACK.getRGB() ? 1 : 0);
								}
								if(sum > 50) break janela;
							}
						}
						if (sum < 50) {
							for (int i = -window*3; i < window*3; i++)
								for (int j = -window*3; j < window*3; j++)
									if (c + i > 0 && r + j > 0 && c + i < image.getWidth() && r + j < image.getHeight())
										base.setRGB(c + i, r + j, Color.WHITE.getRGB());
						}
					}
				}
			}
		}
		return base;
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
	public static BufferedImage binarizeImageAround(BufferedImage img, double threshold) {
		BufferedImage ret = null;
		if (img != null) {
			int gray, r, g, b, rgb;
			ret = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			for (int row = 0; row < img.getHeight(); row++) {
				for (int col = 0; col < img.getWidth(); col++) {
					rgb = img.getRGB(col, row);
					r = (rgb >> 16) & 0xFF;
					g = (rgb >> 8) & 0xFF;
					b = (rgb & 0xFF);
					gray = (r + g + b) / 3;
					if (gray < threshold) {
						ret.setRGB(col, row, Color.BLACK.getRGB());
					} else {
						ret.setRGB(col, row, Color.WHITE.getRGB());
					}
				}
			}
		}
		return ret;
	}
	
	public static BufferedImage binarizeImage(BufferedImage img, int threshold) {
		BufferedImage ret = null;
		if (img != null) {
			int gray, r, g, b, rgb;
			ret = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			for (int row = 0; row < img.getHeight(); row++) {
				for (int col = 0; col < img.getWidth(); col++) {
					rgb = img.getRGB(col, row);
					r = (rgb >> 16) & 0xFF;
					g = (rgb >> 8) & 0xFF;
					b = (rgb & 0xFF);
					gray = (r + g + b) / 3;
					if (gray < threshold) {
						ret.setRGB(col, row, Color.BLACK.getRGB());
					} else {
						ret.setRGB(col, row, Color.WHITE.getRGB());
					}
				}
			}
		}
		return ret;
	}

	public static BufferedImage binarizeImage(BufferedImage img) {
		BufferedImage ret = null;
		if (img != null) {
			int gray, r, g, b, rgb;
			ret = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			for (int row = 0; row < img.getHeight(); row++) {
				for (int col = 0; col < img.getWidth(); col++) {
					rgb = img.getRGB(col, row);
					r = (rgb >> 16) & 0xFF;
					g = (rgb >> 8) & 0xFF;
					b = (rgb & 0xFF);
					gray = (r + g + b) / 3;
					if (gray < 150) {
						ret.setRGB(col, row, Color.BLACK.getRGB());
					} else {
						ret.setRGB(col, row, Color.WHITE.getRGB());
					}
				}
			}
		}
		ret = TransformImage.cutMinMaxPixels(ret);
		return ret;
	}

	public static void dilate(BufferedImage image) {
		if (image != null) {
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRGB(c, r) == Color.BLACK.getRGB()) {
						if (c > 0 && image.getRGB(c - 1, r) == Color.WHITE.getRGB())
							image.getRaster().setSample(c - 1, r, 0, 2);
						if (r > 0 && image.getRGB(c, r - 1) == Color.WHITE.getRGB())
							image.getRaster().setSample(c, r - 1, 0, 2);
						if (c + 1 < image.getWidth() && image.getRGB(c + 1, r) == Color.WHITE.getRGB())
							image.getRaster().setSample(c + 1, r, 0, 2);
						if (r + 1 < image.getHeight() && image.getRGB(c, r + 1) == Color.WHITE.getRGB())
							image.getRaster().setSample(c, r + 1, 0, 2);
					}
				}
			}
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRaster().getSample(c, r, 0) == 2) {
						image.setRGB(c, r, Color.BLACK.getRGB());
					}
				}
			}
		}
	}
	
	public static void dilate3(BufferedImage image) {
		if (image != null) {
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRGB(c, r) == Color.WHITE.getRGB()) {
						if (c > 0 && image.getRGB(c - 1, r) == Color.BLACK.getRGB())
							image.getRaster().setSample(c - 1, r, 0, 2);
						if (r > 0 && image.getRGB(c, r - 1) == Color.BLACK.getRGB())
							image.getRaster().setSample(c, r - 1, 0, 2);
						if (c + 1 < image.getWidth() && image.getRGB(c + 1, r) == Color.BLACK.getRGB())
							image.getRaster().setSample(c + 1, r, 0, 2);
						if (r + 1 < image.getHeight() && image.getRGB(c, r + 1) == Color.BLACK.getRGB())
							image.getRaster().setSample(c, r + 1, 0, 2);
					}
				}
			}
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRaster().getSample(c, r, 0) == 2) {
						image.setRGB(c, r, Color.WHITE.getRGB());
					}
				}
			}
		}
	}

	public static void erode2(BufferedImage image) {
		if (image != null) {
			boolean erode;
			int kernel = 1;
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRGB(c, r) == Color.BLACK.getRGB()) {
						erode = false;
						for (int i = -kernel; i <= kernel; i++) {
							for (int j = -kernel; j <= kernel; j++) {
								erode |= r + i > 0 && c + j > 0 && r + i < image.getHeight() && c + j < image.getWidth()
										&& image.getRGB(c + j, r + i) == Color.WHITE.getRGB();
							}
						}
						if (erode) {
							image.getRaster().setSample(c, r, 0, 2);
						}
					}
				}
			}
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRaster().getSample(c, r, 0) == 2) {
						image.setRGB(c, r, Color.WHITE.getRGB());
					}
				}
			}
		}
	}

	public static void erode(BufferedImage image) {
		if (image != null) {
			boolean erode;
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRGB(c, r) == Color.WHITE.getRGB()) {
						erode = false;
						for (int i = -1; i <= 1; i++) {
							for (int j = -1; j <= 1; j++) {
								erode |= r + i > 0 && c + j > 0 && r + i < image.getHeight() && c + j < image.getWidth()
										&& image.getRGB(c + j, r + i) == Color.BLACK.getRGB();
							}
						}
						if (erode) {
							image.getRaster().setSample(c, r, 0, 2);
						}
					}
				}
			}
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if (image.getRaster().getSample(c, r, 0) == 2) {
						image.setRGB(c, r, Color.BLACK.getRGB());
					}
				}
			}
		}
	}

	public static List<KnnPoint> knn(List<KnnPoint> points, double k) {
		List<KnnPoint> keypoints = new ArrayList<KnnPoint>();
		Queue<KnnPoint> toRemove = new LinkedList<KnnPoint>();
		PriorityQueue<KnnPoint> queue = new PriorityQueue<KnnPoint>(10, new KnnPointComparator());
		for (KnnPoint kp : points) {
			queue.add(kp);
		}
		KnnPoint point = null, next = null;
		boolean endCompared = false;
		while (!endCompared) {
			while (!toRemove.isEmpty()) {
				queue.remove(toRemove.remove());
			}
			point = queue.remove();
			keypoints.add(point);
			Iterator<KnnPoint> itPoint = queue.iterator();
			endCompared = true;
			while (itPoint.hasNext()) {
				next = itPoint.next();
				if (next.isCompared() && point.distance(next) < k) {
					endCompared = false;
					toRemove.add(next);
					break;
				}
			}
		}
		return keypoints;
	}

	public static List<KnnPoint> knn(BufferedImage image, double k) {
		List<KnnPoint> keypoints = new ArrayList<KnnPoint>();
		Queue<KnnPoint> toRemove = new LinkedList<KnnPoint>();
		if (image != null) {
			int gray = 0;
			PriorityQueue<KnnPoint> queue = new PriorityQueue<KnnPoint>(10, new KnnPointComparator());
			KnnPoint lastPointAdded = null;
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					if(lastPointAdded != null && lastPointAdded.distance(c, r) < k)
						continue;

					gray = image.getRGB(c, r) & 0xFF;
					if (gray == 0) {
						lastPointAdded = new KnnPoint(c, r);
						queue.add(lastPointAdded);
					}
				}
			}
			KnnPoint point = null, next = null;
			while (!queue.isEmpty()) {
				while (!toRemove.isEmpty()) {
					queue.remove(toRemove.remove());
				}
				if (!queue.isEmpty()) {
					point = queue.remove();
					keypoints.add(point);
					Iterator<KnnPoint> itPoint = queue.iterator();
					while (itPoint.hasNext()) {
						next = itPoint.next();
						if (point.distance(next) < k) {
							toRemove.add(next);
						}
					}
				}
			}
		}
		return keypoints;
	}

	public static void fillHistogram(double[][] radius, List<KnnPoint> points, double[][] shapeContext) {
		double[][] theta = new double[points.size()][points.size()];
		double[] rBinsEdge = new double[10];
		double mean = 0;
		double rOuter = 2, rInner = 1.0 / 8.0;
		int nBinsR = 5, nBinsTheta = 12;
		// Bin edges
		double nDist = (Math.log10(rOuter) - Math.log10(rInner)) / (nBinsR - 1);
		for (int i = 0; i < nBinsR; i++) {
			rBinsEdge[i] = Math.pow(10, Math.log10(rInner) + nDist * i);
		}

		// BufferedImage result = new BufferedImage(800, 350,
		// BufferedImage.TYPE_USHORT_GRAY);
		// Graphics g = result.getGraphics();
		// g.setColor(new Color(120, 120, 120));
		int valid = 0, diameter = 100;
		for (int i = 0; i < points.size(); i++) {

			// if ((points.get(i).getX() == 174 && points.get(i).getY() == 243)
			// || (points.get(i).getX() == 205 && points.get(i).getY() == 208))
			// g.drawOval(points.get(i).getX(), points.get(i).getY(), 1, 1);

			for (int j = 0; j < points.size(); j++) {
				if (i == j)
					continue;

				if (points.get(i).distance(points.get(j)) > diameter / 2) {
					radius[i][j] = -1;
					continue;
				}
				// Theta
				if (i == j) {
					theta[i][j] = 0;
				} else {
					theta[i][j] = Math.atan2(points.get(j).getY() - points.get(i).getY(),
							points.get(j).getX() - points.get(i).getX());
					// 0 a 2pi
					theta[i][j] = ((theta[i][j] % (2 * Math.PI)) + Math.PI * 2) % (Math.PI * 2);
					// floor theta
					theta[i][j] = (nBinsTheta - 1) - Math.floor(theta[i][j] * nBinsTheta / (2 * Math.PI));
				}
				// Euclidean
				radius[i][j] = points.get(i).distance(points.get(j));

				// g.drawOval(points.get(j).getX(), points.get(j).getY(), 1, 1);
				// g.drawLine(points.get(i).getX(), points.get(i).getY(), points.get(j).getX(),
				// points.get(j).getY());

				mean += radius[i][j];
				valid++;
			}
			// System.out.println("Pontos validos "+valid);
			// if ((points.get(i).getX() == 174 && points.get(i).getY() == 243)
			// || (points.get(i).getX() == 205 && points.get(i).getY() == 208)) {
			// g.setColor(new Color(255, 255, 255));
			// g.drawOval(points.get(i).getX() - diameter / 2, points.get(i).getY() -
			// diameter / 2, diameter,
			// diameter);
			// for (double t = 0.0; t < Math.PI * 2 + 0.5; t += Math.PI / 6) {
			// g.drawLine(points.get(i).getX(), points.get(i).getY(),
			// points.get(i).getX() + (int) ((diameter / 2) * Math.cos(t)),
			// points.get(i).getY() + (int) ((diameter / 2) * Math.sin(t)));
			// }
			// g.setColor(new Color(120, 120, 120));
			// }
			// }
		}

		// File o = new File("ponto" + points.get(0).getX() + "-" + points.get(0).getY()
		// + ".png");
		// try {
		// ImageIO.write(result, "png", o);
		// } catch (IOException e) {
		// }

		mean /= valid;
		int k;
		for (int i = 0; i < points.size(); i++) {
			for (int j = 0; j < points.size(); j++) {
				if (i == j)
					continue;
				if (radius[i][j] < 0)
					continue;
				radius[i][j] /= mean;

				for (k = 0; k < nBinsR - 1; k++)
					if (radius[i][j] <= rBinsEdge[k])
						break;
				radius[i][j] = k;
			}
		}

		// Counting points
		for (int i = 0; i < points.size(); i++) {
			for (int j = 0; j < points.size(); j++) {
				if (i == j)
					continue;
				if (points.get(j).isOutlier())
					continue;
				if (radius[i][j] < 0)
					continue;
				shapeContext[i][(int) (radius[i][j] * nBinsTheta + theta[i][j])]++;
			}
		}
	}

	public static void shapeDescriptor(List<KnnPoint> pointsA, double[][] shapeContextA, List<KnnPoint> pointsB,
			double[][] shapeContextB) {
		if (pointsA != null && pointsB != null) {
			double[][] radiusA = new double[pointsA.size()][pointsA.size()];
			double[][] radiusB = new double[pointsB.size()][pointsB.size()];

			fillHistogram(radiusA, pointsA, shapeContextA);
			fillHistogram(radiusB, pointsB, shapeContextB);
		}
	}

	public static double[][] histCount(List<KnnPoint> pointsA, double[][] shapeContextA, List<KnnPoint> pointsB,
			double[][] shapeContextB) {
		// Normalization
		int i, j, k;
		double nsum, eps = 2.2204e-016;
		int dimension = Math.max(shapeContextA.length, shapeContextB.length);
		double[][] cost = new double[dimension][dimension];

		for (i = 0; i < dimension; i++)
			for (j = 0; j < dimension; j++)
				cost[i][j] = 10e+5;

		for (i = 0; i < pointsA.size(); i++) {
			nsum = eps;
			for (j = 0; j < shapeContextA[0].length; j++)
				nsum += shapeContextA[i][j];
			for (j = 0; j < shapeContextA[0].length; j++)
				shapeContextA[i][j] /= nsum;
		}

		for (i = 0; i < pointsB.size(); i++) {
			nsum = eps;
			for (j = 0; j < shapeContextB[0].length; j++)
				nsum += shapeContextB[i][j];
			for (j = 0; j < shapeContextB[0].length; j++)
				shapeContextB[i][j] /= nsum;
		}

		// Calculate distance
		for (i = 0; i < pointsA.size(); i++) {
			for (j = 0; j < pointsB.size(); j++) {
				nsum = 0;
				for (k = 0; k < shapeContextA[0].length; k++) {
					nsum += (shapeContextA[i][k] - shapeContextB[j][k]) * (shapeContextA[i][k] - shapeContextB[j][k])
							/ (shapeContextA[i][k] + shapeContextB[j][k] + eps);
				}
				cost[i][j] = nsum / 2;
			}
		}

		return cost;
	}

	public static BufferedImage scale(BufferedImage src, int w, int h, int imageType) {
		BufferedImage img = new BufferedImage(w, h, imageType);
		int x, y;
		int ww = src.getWidth();
		int hh = src.getHeight();
		int[] ys = new int[h];
		for (y = 0; y < h; y++)
			ys[y] = y * hh / h;
		for (x = 0; x < w; x++) {
			int newX = x * ww / w;
			for (y = 0; y < h; y++) {
				int col = src.getRGB(newX, ys[y]);
				img.setRGB(x, y, col);
			}
		}
		return img;
	}

	public static BufferedImage skeletonization(BufferedImage image, int iterations) {
		BufferedImage ret = image;
		while (iterations-- > 0 && ret != null) {
			ret = skeletonization(ret, true);
			if (ret != null) {
				image = ret;
				ret = skeletonization(ret, false);
				if (ret != null)
					image = ret;
			}
		}
		return ret == null ? image : ret;
	}

	private static BufferedImage skeletonization(BufferedImage image, boolean step1) {
		BufferedImage ret = null;
		boolean fim = true;
		if (image != null) {
			ret = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
			float window[][] = new float[3][3];
			int sum = 0;
			int NP1, SP1, gray;
			boolean eliminate = true;
			for (int r = 0; r < image.getHeight(); r++) {
				for (int c = 0; c < image.getWidth(); c++) {
					ret.setRGB(c, r, Color.BLACK.getRGB());
					eliminate = true;
					sum = 0;
					gray = image.getRGB(c, r) & 0xFF;
					if (gray == 0 || r == 0 || c == 0 || r == image.getHeight() - 1 || c == image.getWidth() - 1)
						continue;

					for (int i = -1; i <= 1; i++)
						for (int j = -1; j <= 1; j++) {
							if (r + i > 0 && r + i < image.getHeight() && c + j > 0 && c + j < image.getWidth()) {
								gray = image.getRGB(c + j, r + i) & 0xFF;
								window[1 + i][1 + j] = gray / 255;
								sum += window[1 + i][1 + j];
							}
						}
					NP1 = sum;
					SP1 = (window[0][2] > window[0][1] ? 1 : 0) + (window[1][2] > window[0][2] ? 1 : 0)
							+ (window[2][2] > window[1][2] ? 1 : 0) + (window[2][1] > window[2][2] ? 1 : 0)
							+ (window[2][0] > window[2][1] ? 1 : 0) + (window[1][0] > window[2][0] ? 1 : 0)
							+ (window[0][0] > window[1][0] ? 1 : 0) + (window[0][1] > window[0][0] ? 1 : 0);
					eliminate &= (NP1 >= 2 && NP1 <= 6); // a
					eliminate &= (SP1 == 1); // b
					if (step1) {
						eliminate &= (window[0][1] * window[1][2] * window[2][1] == 0); // c
						eliminate &= (window[1][2] * window[2][1] * window[0][1] == 0); // d
					} else {
						eliminate &= (window[0][1] * window[1][2] * window[1][0] == 0); // c'
						eliminate &= (window[0][1] * window[2][1] * window[1][0] == 0); // d'
					}

					if (!eliminate) {
						ret.setRGB(c, r, Color.WHITE.getRGB());
					}
					if (image.getRGB(c, r) != ret.getRGB(c, r)) {
						fim = false;
					}
				}
			}
		}
		return fim ? null : ret;
	}
	
	public static BufferedImage[] splitMiddle(BufferedImage base) {
		if (base != null) {
			return new BufferedImage[] {
					base.getSubimage(0, 0, base.getWidth(), base.getHeight()/2),
					base.getSubimage(0, base.getHeight()/2, base.getWidth(), base.getHeight()/2)
			};
		}
		return null;
	}
	
	public static BufferedImage cropRegionImage(BufferedImage base, BufferedImage filtered) {
		if (base != null) {
			int minCol = base.getWidth(), minRow = base.getHeight(), maxCol = 0, maxRow = 0, gray;
			for (int row = 0; row < base.getHeight(); row++) {
				for (int col = 0; col < base.getWidth(); col++) {
					gray = filtered.getRGB(col, row) & 0XFF;
					if (gray == 0) {
						if (col < minCol)
							minCol = col;
						if (col > maxCol)
							maxCol = col;
						if (row < minRow)
							minRow = row;
						if (row > maxRow)
							maxRow = row;
					}
				}
			}
			return base.getSubimage(minCol, minRow, maxCol-minCol, maxRow-minRow);
		}
		return null;
	}

	public static BufferedImage cutMinMaxPixels(BufferedImage image) {
		if (image != null) {
			int minCol = image.getWidth(), minRow = image.getHeight(), maxCol = 0, maxRow = 0, gray;
			for (int row = 0; row < image.getHeight(); row++) {
				for (int col = 0; col < image.getWidth(); col++) {
					gray = image.getRGB(col, row) & 0XFF;
					if (gray == 0) {
						if (col < minCol)
							minCol = col;
						if (col > maxCol)
							maxCol = col;
						if (row < minRow)
							minRow = row;
						if (row > maxRow)
							maxRow = row;
					}
				}
			}
			int offset = 0;
			BufferedImage img = new BufferedImage(maxCol - minCol + offset, maxRow - minRow + offset, image.getType());
			for (int c = 0; c < img.getWidth(); c++) {
				for (int r = 0; r < img.getHeight(); r++) {
					img.setRGB(c, r, Color.WHITE.getRGB());
				}
			}
			for (int c = 0; c < img.getWidth() - offset; c++) {
				for (int r = 0; r < img.getHeight() - offset; r++) {
					img.setRGB(c + offset / 2, r + offset / 2, image.getRGB(c + minCol, r + minRow));
				}
			}
			image = img;
		}
		return image;
	}

}
