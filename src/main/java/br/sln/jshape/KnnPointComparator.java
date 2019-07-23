package br.sln.jshape;

import java.util.Comparator;

public class KnnPointComparator implements Comparator<KnnPoint> {
	
	@Override
	public int compare(KnnPoint o1, KnnPoint o2) {
		double m1 = Math.sqrt( Math.pow(o1.getX(), 2) + Math.pow(o1.getY(), 2));
		double m2 = Math.sqrt( Math.pow(o2.getX(), 2) + Math.pow(o2.getY(), 2));
		if (m1 < m2){
            return -1;
        }
        if (m1 > m2){
            return 1;
        }
		return 0;
	}

}
