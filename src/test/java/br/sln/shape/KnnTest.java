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
		String[] query = new String[]{""};

		System.out.println("Base\tQuery\t+\t-\tScore");
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
			
			base = TransformImage.scale(base, (int)(Math.max(base.getWidth(), query.getWidth())*2.5), (int)(Math.max(base.getHeight(), base.getHeight())*2.5), base.getType());
			query = TransformImage.scale(query, Math.max(base.getWidth(), query.getWidth()), Math.max(base.getHeight(), base.getHeight()), query.getType());
			
			BufferedImage imgBase = TransformImage.skeletonization(base);
			BufferedImage imgQuery = TransformImage.skeletonization(query);
			
			List<KnnPoint> pontos = TransformImage.knn(imgBase);
			List<KnnPoint> pontosQ = TransformImage.knn(imgQuery);
			
//			System.out.println(pontos.get(0).getX()+","+pontos.get(0).getY()+" vs "+pontosQ.get(32).getX()+","+pontosQ.get(32).getY());
			
			double[][]  shapeContextA = new double[Math.max(pontos.size(), pontos.size())][5*12], 
						shapeContextB = new double[Math.max(pontosQ.size(), pontosQ.size())][5*12];
			TransformImage.shapeDescriptor(pontos, shapeContextA, pontosQ, shapeContextB);
			
			
//			for (int r = 4; r >= 0; r--) {
//				for (int t = 0; t < 12; t++) {
//					System.out.print((int)shapeContextA[0][12*r+t]+"\t");
//				}
//				System.out.println("");
//			}
//			System.out.println("");
//			for (int r = 4; r >= 0; r--) {
//				for (int t = 0; t < 12; t++) {
//					System.out.print((int)shapeContextB[32][12*r+t]+"\t");
//				}
//				System.out.println("");
//			}
//			System.out.println("------------");
			
			double[][] cost = TransformImage.histCount(shapeContextA, shapeContextB);
			
			if( pontos.size() > pontosQ.size() ) {
				Outlier.setOutlierCost(cost, pontos);
			}else if(pontos.size() < pontosQ.size()) {
				Outlier.setOutlierCost(cost, pontosQ);
			}
			
			Hungarian h = new Hungarian(cost);
			int[] resultHungarian = h.execute();
			
//			BufferedImage result = new BufferedImage(Math.max(imgBase.getWidth(), imgQuery.getWidth()), imgBase.getHeight()+imgQuery.getHeight(), imgBase.toBufferedImage().getType());
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
//			g.drawImage(base, 0, 0, null);
//			g.drawImage(query, 0, base.getHeight(), null);
			
//			g.setColor(new Color(120,120,120));
			int j;
			double match=0, unmatch=0;
			for (int i = 0; i < resultHungarian.length; i++) {
				j = resultHungarian[i];
				if(i < pontos.size() && j < pontosQ.size()) {
//					g.drawOval(pontos.get(i).getX(), pontos.get(i).getY(), 1, 1);
//					g.drawOval(pontosQ.get(j).getX(), base.getHeight()+pontosQ.get(j).getY(), 1, 1);
//					System.out.println(i+" "+resultHungarian[i]+" "+cost[i][resultHungarian[i]]);
					if(cost[i][resultHungarian[i]] < 0.15 && pontos.get(i).distance(pontosQ.get(j)) < 100) {
//						g.drawLine(pontos.get(i).getX(), pontos.get(i).getY(), pontosQ.get(j).getX(), base.getHeight()+pontosQ.get(j).getY());
						match += cost[i][j];
					}else {
						unmatch += cost[i][j]; 
					}
//					g.drawString("("+pontos.get(i).getX()+","+pontos.get(i).getY()+")", pontos.get(i).getX(), pontos.get(i).getY());
//					g.drawString("("+pontosQ.get(j).getX()+","+pontosQ.get(j).getY()+")", pontosQ.get(j).getX(), imgBase.getHeight()+pontosQ.get(j).getY());
				}
			}
			DecimalFormat df = new DecimalFormat("0.00");
			
			System.out.println(b.replace("data/", "").substring(0,6)+"\t"+q.replace("data/", "").substring(0,6)+"\t"+df.format(match)+"\t"+df.format(unmatch)+"\t"+df.format((match/(unmatch+match))*100));
			
//			File o = new File("knn.png");
//			ImageIO.write(result, "png", o);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
