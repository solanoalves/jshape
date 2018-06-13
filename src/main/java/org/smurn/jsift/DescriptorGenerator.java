package org.smurn.jsift;

import java.util.Arrays;

public class DescriptorGenerator {
	public static int cont = 0;
	public static double[] generate(double orientation, Image image, int centerX, int centerY, double sigma) {
		//descriptor
		double[][] mag, theta;
		mag = new double[16][16];
		theta = new double[16][16];
		double[] desc = new double[128];
		Arrays.fill(desc, 0.0);
		int offset = 0, ci,ri, c=0, r=-1, cRot=0, rRot=0;
		double rad = orientation * Math.PI/18;
		double cos = Math.cos(-rad);
		double sen = Math.sin(-rad);
		
//		DecimalFormat df = new DecimalFormat("0.00");
		
//		BufferedImage bi = image.toBufferedImage();
//		Graphics g1 = bi.getGraphics();
//		g1.setColor(new Color(255,255,255));
//		g1.drawOval(centerX-15, centerY-15, 30, 30);
//		System.out.println("");
//		System.out.println(orientation*10+"°");
//		System.out.println("");
		
//		System.out.println("("+centerX+","+centerY+")");
		for(int row = centerY - 8; row < centerY+8; row++) {
			r++;
			c=-1;
			for(int col = centerX-8; col < centerX+8; col++) {
				c++;
				if( !(c >= 0 && c < 16 && r >= 0 && r < 16) ) {
					continue;
				}
				
				if(row < 0 || col < 0) continue;
				
//				System.out.println("("+row+","+col+") -> ("+rRot+","+cRot+")");
				
				ri = row-centerY;
				ci = col-centerX;
				rRot = centerY + (int)(ri*cos + ci*sen);
				cRot = centerX + (int)(ci*cos - ri*sen);
				
				if((rRot+1 > image.getHeight()-1) || (rRot-1 < 0) || (cRot+1 > image.getWidth()-1) || (cRot-1 < 0)) continue;
				
				mag[r][c] = Math.floor(Math.sqrt( 
								Math.pow(image.getPixel(rRot, cRot+1)-image.getPixel(rRot, cRot-1), 2) 
							  + Math.pow(image.getPixel(rRot-1, cRot)-image.getPixel(rRot+1, cRot), 2) 
							) * 100) / 100;
				theta[r][c] = Math.atan2(
								(getRotatedPixel(image, centerX, centerY, col, row-1, cos, sen) - getRotatedPixel(image, centerX, centerY, col, row+1, cos, sen))
								,
								(getRotatedPixel(image, centerX, centerY, col+1, row, cos, sen) - getRotatedPixel(image, centerX, centerY, col-1, row, cos, sen))
							  );

//				double fac = KeypointsGenerator.gaussianCircularWeight(r-mag.length/2+0.5, c-mag[0].length/2+0.5, sigma);

				offset = (4*(r/4))*8 + (c/4)*8;
				desc[ offset + radianToBin(theta[r][c]) ] += mag[r][c] * KeypointsGenerator.gaussianCircularWeight(r-mag.length/2+0.5, c-mag[0].length/2+0.5, sigma);
				
//				if((row == centerY+7 && col==centerX+7)) {
//					g1.setColor(new Color(0,0,0));
//					g1.drawLine(centerX, centerY, cRot, rRot);
//					
//					System.out.print("Valor rotacionado: "+mag[r][c]+"\t");
//				}else {
//					g1.setColor(new Color(255,255,255));
//					g1.drawRect(col, row, 1, 1);
//				}
				
				
//				g1.drawLine(col, row, (int)(col + (r/2)*Math.cos(kb.getDirection()*(Math.PI/18.0))), (int)(kb.getPoint().getY() + (r/2)*Math.sin(kb.getDirection()*(Math.PI/18.0))));
				
//				if(row == centerY &&col == centerX)
//					System.out.print(df.format(Math.toDegrees(theta[r][c]))+"*\t");
//				else
//					System.out.print(df.format(Math.toDegrees(theta[r][c]))+"\t");
				
//				System.out.print(df.format(mag[r][c])+"\t");
//				System.out.print(radianToBin(theta[r][c])+"\t");
//				System.out.print(df.format(getRotatedPixel(image, centerX, centerY, row, col, cos, sen))+"\t");
//				System.out.print((offset + radianToBin(theta[r][c]))+"\t");
				
//				if(desc[offset + radianToBin(theta[r][c])] > 0.2)
//					desc[offset + radianToBin(theta[r][c])] = 0.2;
			}	
//			System.out.println("");
		}
		
//		File outputfile = new File("descriptor"+(cont++)+".png");
//		try {
//			ImageIO.write(bi, "png", outputfile);
//		} catch (IOException e) {
//		}
		
		normalize(desc);
		
//		for(int i=0; i<desc.length; i++) {
//			System.out.println(i+"\t"+df.format(desc[i]));
//		}
		
		return desc;
	}
	
	private static float getRotatedPixel(Image image, int centerCol, int centerRow, int col, int row, double cos, double sen) {
		//move to keypoint center and rotate
		int ri = row-centerRow;
		int ci = col-centerCol;
		int rRot = centerRow + (int)(ri*cos + ci*sen);
		int cRot = centerCol + (int)(ci*cos - ri*sen);
		
		if(rRot+1 < image.getHeight() && rRot-1 > -1 && cRot+1 < image.getWidth() && cRot-1 > -1) 
			return image.getPixel(rRot, cRot);
		else return 0.0f;
	}
	
	private static int radianToBin(double radian) {
		radian = radian % (2*Math.PI);
		radian = ((radian < 0.0 ? (2.0*Math.PI+radian) : radian));
		int bin = (int) (radian / (Math.PI/4.0));
		bin = bin < 8 ? bin : 0;
		return bin;
	}
	
	private static void normalize(double[] toNormalize) {
		double[] _toNormalize = new double[toNormalize.length];
		System.arraycopy( toNormalize, 0, _toNormalize, 0, toNormalize.length );
		
		Arrays.sort(_toNormalize);
		
		for(int d = 0; d<toNormalize.length; d++) {
			toNormalize[d] = ((_toNormalize[_toNormalize.length-1]-_toNormalize[0]) == 0) ? 0 : (toNormalize[d]-_toNormalize[0])/(_toNormalize[_toNormalize.length-1]-_toNormalize[0]);
		}
	}
}
