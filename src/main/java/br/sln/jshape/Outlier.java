package br.sln.jshape;

import java.util.List;

/*
Copyright (C) 2004	Yefeng Zheng

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You are free to use this program for non-commercial purpose.              
If you plan to use this code in commercial applications, 
you need additional licensing: 
please contact zhengyf@cfar.umd.edu
*/

////////////////////////////////////////////////////////////////////////////////////////////////
// File NAME:		Outlier
// File Function:	Using a heuristic rule to label outliers for the first iteration.
//					Used for the experiments on the outlier set of the Chui-Rangarajan data set.
//
//				Developed by: Yefeng Zheng
//			   First created: April 2004
//			University of Maryland, College Park
///////////////////////////////////////////////////////////////////////////////////////////////////

public class Outlier {
	public static void labelOutlier(List<KnnPoint> pontos, double[][] rArray, int numPontos) {
		int i,j,k,m;
		int nNN = 6;
		
		double[][] minDist = new double[pontos.size()][pontos.size()];
		
		for(i=0; i<pontos.size(); i++) {
			for(j=0; j< nNN; j++)
				minDist[i][j] = 1e+10;
			for(j=0; j<pontos.size(); j++) {
				if(j== i) continue;
				if(rArray[i][j] >= minDist[i][nNN-1]) continue;
				for(k=nNN-2; k>=0; k--)
					if(rArray[i][j] > minDist[i][k]) break;
				k++;
				for(m=nNN-2; m>=k; m--)
					minDist[i][m+1] = minDist[i][m];
				minDist[i][k] = rArray[i][j];
			}
		}
		
		double[] sumDist = new double[pontos.size()];
		for(i=0; i<pontos.size(); i++) {
			sumDist[i] = 0;
			for(j=0; j<nNN; j++)
				sumDist[i] += minDist[i][j];
		}
		
		int ptIdx[] = new int[pontos.size()];
		for(i=0; i<pontos.size(); i++)
			ptIdx[i] = i;
		int min;
		for( i=0; i<pontos.size(); i++ )
		{
			min = i;
			for( j=i+1; j<pontos.size(); j++ )
				if( sumDist[j] < sumDist[min] )
					min = j;
			if( min != i )
			{
				double tmp = sumDist[i];
				sumDist[i] = sumDist[min];
				sumDist[min] = tmp;
				int tmpIndex = ptIdx[i];
				ptIdx[i] = ptIdx[min];
				ptIdx[min] = tmpIndex;
			}
		}
		
		//Set outliers
		for( i=0; i<pontos.size(); i++ )
			pontos.get(i).setOutlier(true);
		for( i=0; i<numPontos; i++ )
			pontos.get(ptIdx[i]).setOutlier(false);
	}
	
	public static void setOutlierCost(double [][] cost, List<KnnPoint> pontos){
		int i, j;
		if( cost.length < cost[0].length )
		{
			for( i=0; i<cost[0].length; i++ )
			{
				if( !pontos.get(i).isOutlier() )	continue;
				for( j=0; j<cost.length; j++ )
					cost[j][i] = 1e+10;
			}
		}
		else 
		{
			for( i=0; i<cost.length; i++ )
			{
				if( !pontos.get(i).isOutlier() )	continue;
				for( j=0; j<cost[0].length; j++ )
					cost[i][j] = 1e+10;
			}
		}
	}
}