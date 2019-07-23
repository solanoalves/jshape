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

/**
 * Unit tests for {@link Image}.
 */
public class RegionRest {
	
	@Test
	public void image() {
		
		for(int i= 33 ; i< 34 ; i++) {
		
			BufferedImage base = converterPdf("/home/saguiar/imagep/pdf/form"+(i<10?"0":"")+(i)+".pdf", null);
			
			File p = new File("/home/saguiar/imagep/form"+(i<10?"0":"")+i+".png");
			try {
				ImageIO.write(base, "png", p);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public BufferedImage converterPdf(String pdf, Integer pageN) {
		BufferedImage result = null;
		try {
	        String sourceDir = pdf;

	        File sourceFile = new File(sourceDir);
	        if (sourceFile.exists()) {
	            PDDocument document = PDDocument.load(sourceDir);
	            List<PDPage> list = document.getDocumentCatalog().getAllPages();
	            if(pageN != null) {
	            	result = list.get(pageN).convertToImage(BufferedImage.TYPE_USHORT_GRAY, 200);
	            }else {
		            for (PDPage page : list) {
		            	result = page.convertToImage(BufferedImage.TYPE_USHORT_GRAY, 200);
		                break;
		            }
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
