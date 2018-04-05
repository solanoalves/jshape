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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

import br.sln.jshape.Hungarian;
import br.sln.jshape.KnnPoint;
import br.sln.jshape.TransformImage;

/**
 * Unit tests for {@link Image}.
 */
public class KnnTest {
	@Test
	public void comparaImages() {
		String[] base = new String[]{"00.png", "01.png", "02.png", "03.png", "04.jpg", "05.png", "06.jpg", "07.jpg", "08.jpg", "09.jpg", "10.jpg", "11.jpg", "12.jpg"};
//		String[] base = new String[]{"00.png"};
//		String[] query = new String[]{"01-1.png"};
//		String[] query = new String[]{"c1.png", "c2.png", "c3.png", "d1.png"};

		System.out.println("Base\tQuery\t+\t-\tScore");
		String[] spl = null;
		for(String b : base) {
//			comparar("data/base/"+b,"data/"+query[0]);
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

			base = TransformImage.scale(base, (int)(Math.max(base.getWidth(), query.getWidth())*3), (int)(Math.max(base.getHeight(), query.getHeight())*3), base.getType());
			query = TransformImage.scale(query, Math.max(base.getWidth(), query.getWidth()), Math.max(base.getHeight(), query.getHeight()), query.getType());
			
			base = TransformImage.dilate(base);
			base = TransformImage.dilate(base);
			
			query = TransformImage.erode(query);
			query = TransformImage.erode(query);
			
			BufferedImage imgBase = TransformImage.skeletonization(base, 2);
			BufferedImage imgQuery = TransformImage.skeletonization(query, 2);
//			
//			File fbase = new File("imgBase.png");
//			ImageIO.write(base, "png", fbase);
//			File fquery = new File("imgQuery.png");
//			ImageIO.write(query, "png", fquery);
			
			List<KnnPoint> _pontos = TransformImage.knn(imgBase, 3);
			List<KnnPoint> _pontosQ = TransformImage.knn(imgQuery, 3);
			
			List<Integer> indices = new ArrayList<>();
			for(int i=0; i<Math.min(_pontos.size(), _pontosQ.size()); i++) {
				indices.add(i);
			}
			Collections.shuffle(indices);
			List<KnnPoint> pontos = new ArrayList<>();
			for(int i=0;i<indices.size();i++)
				pontos.add(_pontos.get(i));
			List<KnnPoint> pontosQ = new ArrayList<>();
			for(int i=0;i<indices.size();i++)
				pontosQ.add(_pontosQ.get(i));
			
			
			double[][]  shapeContextA = new double[Math.max(pontos.size(), pontosQ.size())][5*12], 
						shapeContextB = new double[Math.max(pontos.size(), pontosQ.size())][5*12];
			TransformImage.shapeDescriptor(pontos, shapeContextA, pontosQ, shapeContextB);

			double[][] cost = TransformImage.histCount(pontos, shapeContextA, pontosQ, shapeContextB);
			
			Hungarian h = new Hungarian(cost);
			int[] resultHungarian = h.execute();
			
//			BufferedImage result = new BufferedImage(Math.max(imgBase.getWidth(), imgQuery.getWidth()), imgBase.getHeight()+imgQuery.getHeight(), imgBase.getType());
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
			double match=0, unmatch=2.2204e-016;
			for (int i = 0; i < resultHungarian.length; i++) {
				j = resultHungarian[i];
				if(cost[i][j] < 10e5 && pontos.get(i).distance(pontosQ.get(j)) < 60) {
					match+=1;
//					g.drawLine(pontos.get(i).getX(), pontos.get(i).getY(), pontosQ.get(j).getX(), base.getHeight()+pontosQ.get(j).getY());
				}else {
					unmatch += 1; 
				}
			}
			DecimalFormat df = new DecimalFormat("0.00");
			double scoreFactor = (match*unmatch)/(match+unmatch);
			System.out.println(b.replace("data/", "").substring(0,6)+"\t"+q.replace("data/", "").substring(0,6)+"\t"+df.format(match)+"\t"+df.format(unmatch)+"\t"+df.format(scoreFactor*(match/(unmatch+match))*100));
			
//			File ox = new File("knn_"+(b.replace("data/base/", "")+"_"+q.replace("data/", ""))+".png");
//			ImageIO.write(result, "png", ox);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
