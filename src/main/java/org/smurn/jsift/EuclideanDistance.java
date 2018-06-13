package org.smurn.jsift;

public class EuclideanDistance {
	public static double calculate(double[] v, double[] w) throws Exception {
		if(v.length == 0 || w.length == 0) throw new Exception("Vectors must not be empty");
		
		if(v.length != w.length) throw new Exception("Vectors must be same dimension");
		boolean nonzero = false;
		double acc = 0;
		for (int i = 0; i < v.length; i++) {
			if(v[i] > 0 || w[i] > 0) nonzero = true;
			acc += Math.pow(v[i]-w[i], 2);
		}
		return nonzero ? Math.sqrt(acc) : Double.MAX_VALUE;
	}
}
