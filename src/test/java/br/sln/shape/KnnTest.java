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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

import br.sln.jshape.Hungarian;
import br.sln.jshape.KnnPoint;
import br.sln.jshape.Outlier;
import br.sln.jshape.TransformImage;

/**
 * Unit tests for {@link Image}.
 */
public class KnnTest {
	@Test
	public void comparaImages() {
//		String[] query = new String[]{"s0.png","c0.png","s1.png", "s2.png", "s3.png", "s4.png", "s5.png", "f1.png", "f2.png", "f3.png", "f4.png", "f5.png", "cf1.png", "c1.png", "c2.png", "c3.png", "c4.png", "c5.png", "s6.jpg", "f6.png", "ff1.png", "ff2.png", "ff3.png"};
		String[] query = new String[]{"f1.png","c3.png"};

		System.out.println("Base\tpixels\tQuery\tpixels\t+\t-\tScore");
		for(String b : query) {
			for(String q : query) {
				if(!b.equals(q)) {
					comparar("data/"+b,"data/"+q);
				}
			}
		}
	}

	public void comparar(String b, String q) {
		try {
			BufferedImage base = ImageIO.read(new File(b));
			BufferedImage query = ImageIO.read(new File(q));
			
			base = TransformImage.binarizeImage(base);
			query = TransformImage.binarizeImage(query);
			
			base = TransformImage.scale(base, (int)(Math.max(base.getWidth(), query.getWidth())*2.5), (int)(Math.max(base.getHeight(), query.getHeight())*2.5), base.getType());
			query = TransformImage.scale(query, Math.max(base.getWidth(), query.getWidth()), Math.max(base.getHeight(), query.getHeight()), query.getType());
			
			BufferedImage imgBase = TransformImage.skeletonization(base);
			BufferedImage imgQuery = TransformImage.skeletonization(query);
			
			List<KnnPoint> pontos = TransformImage.knn(imgBase);
			List<KnnPoint> pontosQ = TransformImage.knn(imgQuery);
			
			double[][]  shapeContextA = new double[Math.max(pontos.size(), pontos.size())][5*12], 
						shapeContextB = new double[Math.max(pontosQ.size(), pontosQ.size())][5*12];
			TransformImage.shapeDescriptor(pontos, shapeContextA, pontosQ, shapeContextB);
			
			double[][] cost = TransformImage.histCount(shapeContextA, shapeContextB);
			
			if( pontos.size() > pontosQ.size() ) {
				Outlier.setOutlierCost(cost, pontos);
			}else if(pontos.size() < pontosQ.size()) {
				Outlier.setOutlierCost(cost, pontosQ);
			}
			
			Hungarian h = new Hungarian(cost);
			int[] resultHungarian = h.execute();
			
			BufferedImage result = new BufferedImage(Math.max(imgBase.getWidth(), imgQuery.getWidth()), imgBase.getHeight()+imgQuery.getHeight(), imgBase.getType());
			Graphics g = result.getGraphics();
			Graphics2D drawer = result.createGraphics() ;
			drawer.setBackground(Color.WHITE);
			drawer.clearRect(0,0,result.getWidth(),result.getHeight());
			g.setColor(new Color(100,100,100));
			for (int i = 0; i < pontos.size(); i++) {
				g.drawOval(pontos.get(i).getX(), pontos.get(i).getY(), 1, 1);
			}
			for (int i = 0; i < pontosQ.size(); i++) {
				g.drawOval(pontosQ.get(i).getX(), base.getHeight()+pontosQ.get(i).getY(), 1, 1);
			}
			g.setColor(new Color(120,120,120));
			
			int j;
			double match=0, unmatch=0;
			double min = 10000; int mini=0,minj=0;
			for (int i = 0; i < resultHungarian.length; i++) {
				j = resultHungarian[i];
				if(i < pontos.size() && j < pontosQ.size()) {
					if(cost[i][j] < 0.25) {
						if(min > cost[i][j]) {
							min = cost[i][j];
							mini = i;
							minj = j;
						}
						g.drawLine(pontos.get(i).getX(), pontos.get(i).getY(), pontosQ.get(j).getX(), base.getHeight()+pontosQ.get(j).getY());
						match += cost[i][j];
					}else {
						unmatch += cost[i][j]; 
					}
				}
			}
			System.out.println("Ponto minimo: ("+pontos.get(mini).getX()+","+pontos.get(mini).getY()+") -> ("+pontosQ.get(minj).getX()+","+pontosQ.get(minj).getY()+")");
			DecimalFormat df = new DecimalFormat("0.00");
//			if((match/(unmatch+match))*100 > 3.0)
//			if(!b.replace("data/", "").substring(0,1).equals(q.replace("data/", "").substring(0,1)))
				System.out.println(b.replace("data/", "").substring(0,6)+"\t"+(pontos.size())+"\t"+q.replace("data/", "").substring(0,6)+"\t"+(pontosQ.size())+"\t"+df.format(match)+"\t"+df.format(unmatch)+"\t"+df.format((match/(unmatch+match))*100));
			
			File o = new File("knn_"+(new Date().getTime())+".png");
			ImageIO.write(result, "png", o);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
