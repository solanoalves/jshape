package br.sln.shape;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.smurn.jsift.GaussianFilter;

import br.sln.jshape.TransformImage;

public class NeuralNetworkTest {
	
	public static int NEURAL_NET_SIZE = 1836;
	
//	@Test
	public void construirModelo() {
		gerarDataset();
		treinar();
		try {
			test();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void treinar() {
		System.out.println("Iniciando treino");
		NeuralNetwork<BackPropagation> nn = new MultiLayerPerceptron(NEURAL_NET_SIZE, 24, 24, 1);
		BackPropagation backPropagation = new BackPropagation();
		backPropagation.setMaxIterations(1000);
		DataSet dataset = DataSet.createFromFile("/home/saguiar/train/train.txt", NEURAL_NET_SIZE, 1, ",");
		nn.learn(dataset, backPropagation);
		nn.save("/home/saguiar/train/nn");
		System.out.println("Fim treino");
	}
	
	@SuppressWarnings("unchecked")
	public void test() throws IOException {
		NeuralNetwork<BackPropagation> nn = NeuralNetwork.createFromFile("/home/saguiar/train/nn");
		double[] input = new double[NEURAL_NET_SIZE];
		
		//Testando imagem correta
		BufferedImage base = ImageIO.read(new File("/home/saguiar/train/base.png"));
		base = TransformImage.binarizeImage(base, 200);
		int index = 0;
		for(int l=0; l<base.getHeight(); l++) {
			for(int c=0;c<base.getWidth(); c++) {
				input[index++] = (base.getRGB(c, l) == Color.BLACK.getRGB() ? 1.0 : 0.0);
			}
		}
		nn.setInput(input);
		nn.calculate();
		double o[] = nn.getOutput();
		System.out.println("Base Resultou "+o[0]);
		
		//Testando fake
		base = ImageIO.read(new File("/home/saguiar/train/fake/t"+(new Random().nextInt(100))+".png"));
		base = TransformImage.binarizeImage(base, 200);
		index = 0;
		for(int l=0; l<base.getHeight(); l++) {
			for(int c=0;c<base.getWidth(); c++) {
				input[index++] = (base.getRGB(c, l) == Color.BLACK.getRGB() ? 1.0 : 0.0);
			}
		}
		nn.setInput(input);
		nn.calculate();
		o = nn.getOutput();
		System.out.println("Fake Resultou "+o[0]);
	}
	
	public void gerarDataset() {
//		BufferedImage base = converterPdf("/home/saguiar/Downloads/dennys-cnh.pdf");
//		BufferedImage base = converterPdf("/home/saguiar/Downloads/fran-cnh.pdf");
//		BufferedImage base = converterPdf("/home/saguiar/Downloads/solano-cnh.pdf");
//		BufferedImage base = converterPdf("/home/saguiar/Downloads/formident.pdf");
//		BufferedImage base = converterPdf("/home/saguiar/form01.pdf");
		List<String> inputs = new ArrayList<>();
		BufferedImage base = null, base2 = null;
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("/home/saguiar/train/train.txt"));
			base = ImageIO.read(new File("/home/saguiar/train/base.png"));
			org.smurn.jsift.Image tmp = null;
			BufferedImage tmp2 = null;
			GaussianFilter gf = new GaussianFilter();
			double s = 1;
			StringBuilder sb = new StringBuilder();
			Random r = new Random();
			for(int i=0; i<1500; i++) {
				tmp = new org.smurn.jsift.Image(base);
				tmp = gf.filter(tmp, s);
				tmp2 = TransformImage.binarizeImage(tmp.toBufferedImage(), 200);
//				tmp2 = rotate(tmp2, r.nextInt(8)-5);
				File p = new File("/home/saguiar/train/data/"+StringUtils.leftPad(i+"", 3, "0")+".png");
				try {
					ImageIO.write(tmp2, "png", p);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				for(int l=0; l<tmp2.getHeight(); l++) {
					for(int c=0;c<tmp2.getWidth(); c++) {
						sb.append(tmp2.getRGB(c, l) == Color.BLACK.getRGB() ? 1 : 0);
						sb.append(",");
					}
				}
				sb.append("1\n");
				inputs.add(sb.toString());
				s = s + 0.0005;
				
//				//adicionando fakes aleatoriamente
//				if(r.nextInt(100) > 90) {
//					sb.setLength(0);
//					for(int l=0; l<tmp2.getHeight(); l++) {
//						for(int c=0;c<tmp2.getWidth(); c++) {
//							sb.append(tmp2.getRGB(c, l) == Color.BLACK.getRGB() ? 0 : 1);
//							sb.append(",");
//						}
//					}
//					sb.append("0\n");
//					inputs.add(sb.toString());
//				}
				
				sb.setLength(0);
			}
			System.out.println(s);
			//removendo indesejadas
			s = 1;
			for(int iteracoes = 0; iteracoes < 2; iteracoes ++) {
				s = s + 0.04;
				for(int fakes = 1; fakes <= 100; fakes++ ) {
					base2 = ImageIO.read(new File("/home/saguiar/train/fake/t"+fakes+".png"));
					tmp = new org.smurn.jsift.Image(base2);
					tmp = gf.filter(tmp, s);
					tmp2 = TransformImage.binarizeImage(tmp.toBufferedImage(), 200);
					for(int l=0; l<tmp2.getHeight(); l++) {
						for(int c=0;c<tmp2.getWidth(); c++) {
							sb.append(tmp2.getRGB(c, l) == Color.BLACK.getRGB() ? 1 : 0);
							sb.append(",");
						}
					}
					sb.append("0\n");
					inputs.add(sb.toString());
					sb.setLength(0);
				}
			}
			Collections.shuffle(inputs);
			for(String linha : inputs) {
				bw.write(linha);
			}
			System.out.println("total de "+inputs.size()+" entradas");
			bw.close();
			System.out.println(s);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		base = TransformImage.binarizeImage(base, 100);
//		base = TransformImage.minCount(base);
//		org.smurn.jsift.Image image = new org.smurn.jsift.Image(img);
//		GaussianFilter gf = new GaussianFilter();
//		image = gf.filter(image, 5);
//		base = image.toBufferedImage();
//		base = TransformImage.binarizeImage(base, 230);
//		base = TransformImage.minCount(base);
		
//		BufferedImage filtered = TransformImage.minCount(base);
//		TransformImage.dilate(img);
//		BufferedImage filtered = TransformImage.minCount(img);
//		base = TransformImage.cropRegionImage(base, filtered);
//		base = base.getSubimage(70, 473, 250, 45);
		
		File p = new File("/home/saguiar/saida.png");
		try {
			ImageIO.write(base, "png", p);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private BufferedImage rotate(BufferedImage image, int degrees) {
		BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		Graphics2D graphics = bi.createGraphics();
		graphics.setPaint ( new Color ( 255, 255, 255 ) );
		graphics.fillRect ( 0, 0, bi.getWidth(), bi.getHeight() );
		double rotationRequired = Math.toRadians (degrees);
		double locationX = image.getWidth() / 2;
		double locationY = image.getHeight() / 2;
		AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		Graphics g2d = bi.getGraphics();
		g2d.drawImage(op.filter(image, null), 0, 0, null);
		return bi;
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
	            	result = page.convertToImage(BufferedImage.TYPE_USHORT_GRAY, 200);
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
