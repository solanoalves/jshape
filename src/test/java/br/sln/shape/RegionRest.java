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
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;

import br.sln.jshape.KnnPoint;
import br.sln.jshape.TransformImage;

/**
 * Unit tests for {@link Image}.
 */
public class RegionRest {
	
	@Test
	public void imagemcnh() {
		BufferedImage base = converterPdf("/home/saguiar/Downloads/dennys-cnh.pdf");
		TransformImage.minCount(base);
		base = TransformImage.binarizeImage(base, 240);
		base = TransformImage.cutMinMaxPixels(base);
		File p = new File("/home/saguiar/saida.png");
		try {
			ImageIO.write(base, "png", p);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	@Test
	public void comparaImages() {
		BufferedImage base = converterPdf("pdf2.pdf");
		BufferedImage bi = TransformImage.filterBottomImage(base);
		bi = TransformImage.binarizeImage(bi, 200);
		TransformImage.erode2(bi);
		bi = TransformImage.filterBlob(bi);
		List<KnnPoint> pontos = TransformImage.blobPoints(bi);
		
		int offsetY = 10;
		int offsetX = 15;
		int width = (int)((pontos.get(2).getX()-pontos.get(0).getX())*0.95);
		int height = (int)(width*0.145);
		int offsetBox = (int)(width*0.067) + height;
		
		BufferedImage proprietario = base.getSubimage(pontos.get(0).getX()+offsetX, pontos.get(0).getY()+offsetY, width, height);
		BufferedImage condutor = base.getSubimage(pontos.get(0).getX()+offsetX, offsetBox+pontos.get(0).getY()+offsetY, width, height);
		
		File p = new File("proprietario.png");
		File c = new File("condutor.png");
		try {
			ImageIO.write(proprietario, "png", p);
			ImageIO.write(condutor, "png", c);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public BufferedImage converterPdf(String pdf) {
		BufferedImage result = null;
		try {
	        String sourceDir = pdf;

	        File sourceFile = new File(sourceDir);
	        if (sourceFile.exists()) {
	            PDDocument document = PDDocument.load(sourceDir);
	            List<PDPage> list = document.getDocumentCatalog().getAllPages();
	            for (PDPage page : list) {
	            	result = page.convertToImage();
	                break;
	            }
	            document.close();
	        } else {
	            System.err.println(sourceFile.getName() +" File not exists");
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		return result;
	}
	
}
