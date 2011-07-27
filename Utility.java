package com.example;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Utility {

	public static void retrieveData(String fileName, ArrayList<Double> lst) {
		try {
			System.out.println(" retrieve" + fileName);
			FileInputStream fStream = new FileInputStream(fileName);
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				String[] line = strLine.split(",");
				// line[0].replace('[', ' ');
				// line[0].trim();
				String val = line[1].replaceAll("]", "");
				val.trim();
				lst.add(Double.parseDouble(val));

			}
			in.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public static void main(String[] args) {
		ArrayList<Double> runAcValLst = new ArrayList<Double>();
		ArrayList<Double> walkAcValLst = new ArrayList<Double>();
		ArrayList<Double> standAcValLst = new ArrayList<Double>();
		double runAvg, runPeekToPeek;

		Utility
				.retrieveData(
						"//media//5652CD5F52CD4509//UU_SummerJob//samples//standing.file",
						standAcValLst);
		Utility
				.retrieveData(
						"//media//5652CD5F52CD4509//UU_SummerJob//samples//walking.file",
						walkAcValLst);
		Utility
				.retrieveData(
						"//media//5652CD5F52CD4509//UU_SummerJob//samples//running.file",
						runAcValLst);

		System.out.println(" Size stand " + standAcValLst.size());
		System.out.println(" Size walk " + walkAcValLst.size());
		System.out.println(" Size run " + runAcValLst.size());
		Classifier classfr = new Classifier();

		System.out.println("Peak to Peak Stand:"
				+ classfr.computePeakToPeak(standAcValLst));
		System.out.println("Peak to Peak walk:"
				+ classfr.computePeakToPeak(walkAcValLst));
		System.out.println("Peak to Peak Run:"
				+ classfr.computePeakToPeak(runAcValLst));
		

		double runAvgVal = classfr
				.computeAvg(runAcValLst);
		double walkingAvgVal = classfr
				.computeAvg(walkAcValLst);
		double standingAvgVal = classfr
				.computeAvg(standAcValLst);

		double standStdDev = classfr.computeDiffSq(
				standAcValLst, standingAvgVal);
		System.out.println(" Standing Std Dev " + standStdDev);
		
		double walkStdDev = classfr.computeDiffSq(
				walkAcValLst, walkingAvgVal);
		System.out.println(" Walking Std Dev " + walkStdDev);
		
		double runStdDev = classfr.computeDiffSq(
				runAcValLst, runAvgVal);
		System.out.println(" Running Std Dev " + runStdDev);
	
		
	}

}
