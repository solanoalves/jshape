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
package br.sln.shape;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import javax.imageio.ImageIO;

import br.sln.jshape.Hungarian;
import br.sln.jshape.KnnPoint;
import br.sln.jshape.TransformImage;
import ij.process.ByteProcessor;

/**
 * Unit tests for {@link Image}.
 */
public class KnnTest {
//	@Test	
	public void comparaImages() {
		String[] base = new String[]{"00.png", "01.png", "02.png", "03.png", "04.jpg", "05.png", "06.jpg", "07.jpg", "08.jpg", "09.jpg", "10.jpg", "11.jpg", "12.jpg", "13.jpg", "14.jpg", "15.jpg", "16.jpg", "17.jpg", "18.jpg", "19.jpg", "20.jpg"};
//		String[] base = new String[]{"02.png"};
//		String[] query = new String[]{"20-1.png"};
//		String[] query = new String[]{"c1.png", "c2.png", "c3.png", "d1.png"};

		System.out.println("Base\tQuery\t+\t-\tScore");
		String[] spl = null;
		for(String b : base) {
//			comparar(b,query[0]);
			for(String q : base) {
				for(int i=0; i<2; i++) {
					spl = q.split("\\.");
					comparar(b,spl[0]+"-"+i+".png");
				}
			}
		}
	}

	public void comparar(String b, String q) {
		try {
			BufferedImage base = ImageIO.read(new File("data/base/"+b));
			BufferedImage query = ImageIO.read(new File("data/"+q));

			base = TransformImage.binarizeImage(base);
			query = TransformImage.binarizeImage(query);
			
			base = TransformImage.scale(base, Math.max(base.getWidth(), query.getWidth())*2, Math.max(base.getHeight(), query.getHeight())*2, base.getType());
			query = TransformImage.scale(query, base.getWidth(), base.getHeight(), query.getType());

			new ByteProcessor(base).skeletonize();
			new ByteProcessor(query).skeletonize();
			
			List<KnnPoint> pontos = TransformImage.knn(base, 2);
			List<KnnPoint> pontosQ = TransformImage.knn(query, 2);
			
//			BufferedImage imgBase = base;
//			BufferedImage imgQuery = query;
			
//			File fbase = new File("imgBase.png");
//			ImageIO.write(base, "png", fbase);
//			File fquery = new File("imgQuery.png");
//			ImageIO.write(query, "png", fquery);
			
//			List<Integer> indices = new ArrayList<>();
//			for(int i=0; i<Math.min(_pontos.size(), _pontosQ.size()); i++) {
//				indices.add(i);
//			}
//			Collections.shuffle(indices);
//			List<KnnPoint> pontos = new ArrayList<>();
//			for(int i : indices)
//				pontos.add(_pontos.get(i));
//			List<KnnPoint> pontosQ = new ArrayList<>();
//			for(int i : indices)
//				pontosQ.add(_pontosQ.get(i));
			
			double[][]  shapeContextA = new double[Math.max(pontos.size(), pontosQ.size())][5*12], 
						shapeContextB = new double[Math.max(pontos.size(), pontosQ.size())][5*12];
			TransformImage.shapeDescriptor(pontos, shapeContextA, pontosQ, shapeContextB);

			double[][] cost = TransformImage.histCount(pontos, shapeContextA, pontosQ, shapeContextB);
			
			Hungarian h = new Hungarian(cost);
			int[] resultHungarian = h.execute();
			
//			BufferedImage result = new BufferedImage(Math.max(base.getWidth(), query.getWidth()), base.getHeight()+query.getHeight(), base.getType());
//			Graphics g = result.getGraphics();
//			Graphics2D drawer = result.createGraphics() ;
//			drawer.setBackground(Color.WHITE);
//			drawer.clearRect(0,0,result.getWidth(),result.getHeight());
//			g.setColor(new Color(100,100,100));
//			for (int i = 0; i < pontos.size(); i++) {
//				g.drawOval(pontos.get(i).getX(), pontos.get(i).getY(), 1, 1);
//			}
//			for (int i = 0; i < pontosQ.size(); i++) {
//				g.drawOval(pontosQ.get(i).getX(), base.getHeight()+pontosQ.get(i).getY(), 1, 1);
//			}
//			g.setColor(new Color(120,120,120));
			
			int j;
			double match=0, unmatch=2.2204e-016, weight=1.0;
			for (int i = 0; i < resultHungarian.length; i++) {
				j = resultHungarian[i];
				if(cost[i][j] < 0.2 && pontos.get(i).distance(pontosQ.get(j)) < 65) {
					match+= 1;
					weight += Math.abs(0.2-cost[i][j]);
//					g.drawLine(pontos.get(i).getX(), pontos.get(i).getY(), pontosQ.get(j).getX(), base.getHeight()+pontosQ.get(j).getY());
				}else {
					unmatch += 1; 
				}
			}
			DecimalFormat df = new DecimalFormat("0.00");
			double scoreFactor = Math.pow(match/100, 3) + weight*Math.pow(match/100, 2);
			System.out.println(b.replace("data/", "").substring(0,6)+"\t"+q.replace("data/", "").substring(0,6)+"\t"+df.format(match)+"\t"+df.format(unmatch)+"\t"+df.format(scoreFactor*match));
			
//			File ox = new File("knn_"+(b.replace("data/base/", "")+"_"+q.replace("data/", ""))+".png");
//			ImageIO.write(result, "png", ox);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
