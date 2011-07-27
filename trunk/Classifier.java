package com.example;

import java.util.ArrayList;
import java.util.Iterator;

public class Classifier {

	public static String STAND_URL = "//media//5652CD5F52CD4509//UU_SummerJob//samples//standing.file";
	public static String WALK_URL = "//media//5652CD5F52CD4509//UU_SummerJob//samples//walking.file";
	public static String RUN_URL = "//media//5652CD5F52CD4509//UU_SummerJob//samples//running.file";

	private ArrayList<Double> standAccVal;
	private ArrayList<Double> walkAccVal;
	private ArrayList<Double> runAccVal;

	/**
	 * @param runAccVal
	 *            the runningacclerationVal to set
	 */
	public void setRunAccVal() {
		this.runAccVal = new ArrayList<Double>();

		this.runAccVal.add(24.0); // 0.0
		this.runAccVal.add(55.0); // 12.5
		this.runAccVal.add(35.0); // 25.0
		this.runAccVal.add(27.5); // 37.5
		this.runAccVal.add(16.0); // 50.0
		this.runAccVal.add(15.0); // 62.5
		this.runAccVal.add(20.0); // 75.0
		this.runAccVal.add(36.0); // 87.5

		this.runAccVal.add(25.0); // 100
		this.runAccVal.add(16.0); // 112.5
		this.runAccVal.add(13.0); // 125.0
		this.runAccVal.add(42.5); // 137.5
		this.runAccVal.add(32.5); // 150.0
		this.runAccVal.add(33.0); // 162.5
		this.runAccVal.add(17.5); // 175.0
		this.runAccVal.add(12.5); // 187.5
		this.runAccVal.add(32.0); // 200

		this.runAccVal.add(20.0); // 212.5
		this.runAccVal.add(17.0); // 225.0
		this.runAccVal.add(22.5); // 237.5
		this.runAccVal.add(11.0); // 250.0
		this.runAccVal.add(10.0); // 262.5
		this.runAccVal.add(27.0); // 275.0
		this.runAccVal.add(12.5); // 287.5
		this.runAccVal.add(25.0); // 300

		this.runAccVal.add(6.0); // 312.5
		this.runAccVal.add(10.0); // 325.0
		this.runAccVal.add(25.0); // 337.5
		this.runAccVal.add(20.0); // 350.0
		this.runAccVal.add(21.0); // 362.5
		this.runAccVal.add(42.5); // 375.0
		this.runAccVal.add(10.0); // 387.5
		this.runAccVal.add(26.5); // 400

		this.runAccVal.add(10.5); // 412.5
		this.runAccVal.add(30.0); // 425.0
		this.runAccVal.add(10.0); // 437.5
		this.runAccVal.add(21.0); // 450.0
		this.runAccVal.add(18.5); // 462.5
		this.runAccVal.add(12.5); // 475.0
		this.runAccVal.add(24.0); // 487.5
		this.runAccVal.add(32.5);// 500

		this.runAccVal.add(19.0); // 512.5

	}

	/**
	 * @param walkingacclerationVal
	 *            the walkingacclerationVal to set
	 */
	public void setWalkingacclerationVal() {
		this.walkAccVal = new ArrayList<Double>();

		this.walkAccVal.add(13.0); // 0.0
		this.walkAccVal.add(14.0); // 12.5
		this.walkAccVal.add(10.0); // 25.0
		this.walkAccVal.add(8.0); // 37.5
		this.walkAccVal.add(11.0); // 50.0
		this.walkAccVal.add(12.5); // 62.5
		this.walkAccVal.add(12.0); // 75.0
		this.walkAccVal.add(7.5); // 87.5

		this.walkAccVal.add(1.0); // 100
		this.walkAccVal.add(13.0); // 112.5
		this.walkAccVal.add(14.0); // 125.0
		this.walkAccVal.add(8.5); // 137.5
		this.walkAccVal.add(11.0); // 150.0
		this.walkAccVal.add(12.5); // 162.5
		this.walkAccVal.add(10.0); // 175.0
		this.walkAccVal.add(10.0); // 187.5
		this.walkAccVal.add(10.0); // 200

		this.walkAccVal.add(10.0); // 212.5
		this.walkAccVal.add(12.5); // 225.0
		this.walkAccVal.add(8.5); // 237.5
		this.walkAccVal.add(11.0); // 250.0
		this.walkAccVal.add(10.0); // 262.5
		this.walkAccVal.add(14.0); // 275.0
		this.walkAccVal.add(10.0); // 287.5
		this.walkAccVal.add(12.5); // 300

		this.walkAccVal.add(10.0); // 312.5
		this.walkAccVal.add(13.0); // 325.0
		this.walkAccVal.add(7.5); // 337.5
		this.walkAccVal.add(10.5); // 350.0
		this.walkAccVal.add(10.0); // 362.5
		this.walkAccVal.add(12.5); // 375.0
		this.walkAccVal.add(10.0); // 387.5
		this.walkAccVal.add(7.5); // 400

		this.walkAccVal.add(10.5); // 412.5
		this.walkAccVal.add(11.0); // 425.0
		this.walkAccVal.add(12.5); // 437.5
		this.walkAccVal.add(7.5); // 450.0
		this.walkAccVal.add(12.5); // 462.5
		this.walkAccVal.add(10.0); // 475.0
		this.walkAccVal.add(14.0); // 487.5
		this.walkAccVal.add(9.0); // 500

		this.walkAccVal.add(11.0); // 512.5

	}

	/**
	 * @param acclerationVal
	 *            the acclerationVal to set
	 */
	public void setAcclerationVal() {
		this.standAccVal = new ArrayList<Double>();

		this.standAccVal.add(7.0); // 0.0
		this.standAccVal.add(1.0); // 12.5
		this.standAccVal.add(5.0); // 25.0
		this.standAccVal.add(2.5); // 37.5
		this.standAccVal.add(2.0); // 50.0
		this.standAccVal.add(15.0); // 62.5
		this.standAccVal.add(2.0); // 75.0
		this.standAccVal.add(30.0); // 87.5

		this.standAccVal.add(1.0); // 100
		this.standAccVal.add(41.0); // 112.5
		this.standAccVal.add(2.0); // 125.0
		this.standAccVal.add(47.5); // 137.5
		this.standAccVal.add(4.0); // 150.0
		this.standAccVal.add(36.0); // 162.5
		this.standAccVal.add(2.0); // 175.0
		this.standAccVal.add(15.0); // 187.5
		this.standAccVal.add(1.0); // 200

		this.standAccVal.add(5.0); // 212.5
		this.standAccVal.add(2.5); // 225.0
		this.standAccVal.add(47.5); // 237.5
		this.standAccVal.add(5.0); // 250.0
		this.standAccVal.add(5.0); // 262.5
		this.standAccVal.add(40.0); // 275.0
		this.standAccVal.add(4.0); // 287.5
		this.standAccVal.add(42.5); // 300

		this.standAccVal.add(4.0); // 312.5
		this.standAccVal.add(40.0); // 325.0
		this.standAccVal.add(4.5); // 337.5
		this.standAccVal.add(37.5); // 350.0
		this.standAccVal.add(1.0); // 362.5
		this.standAccVal.add(15.0); // 375.0
		this.standAccVal.add(1.25); // 387.5
		this.standAccVal.add(4.0); // 400

		this.standAccVal.add(2.5); // 412.5
		this.standAccVal.add(5.0); // 425.0
		this.standAccVal.add(1.25); // 437.5
		this.standAccVal.add(5.0); // 450.0
		this.standAccVal.add(10.0); // 462.5
		this.standAccVal.add(2.5); // 475.0
		this.standAccVal.add(20.0); // 487.5
		this.standAccVal.add(2.0); // 500
		this.standAccVal.add(29.0); // 512.5

	}

	public static void main(String[] args) {
		ArrayList<Double> runAcValLst = new ArrayList<Double>();
		ArrayList<Double> walkAcValLst = new ArrayList<Double>();
		ArrayList <Double> standAcValLst = new ArrayList<Double>();
		
		double runAvg, runStdDev, runPToP;
		double walkAvg, walkStdDev, walkPToP;
		double standAvg, standStdDev, standPToP;
		
		double runDataAvg, runDataStdDev, runDataPeekToPeek;
		double walkDataAvg, walkDataStdDev, walkDataPeekToPeek;
		double standDataAvg, standDataStdDev, standDataPeekToPeek;
		
		//Load the Known default pattern
		Classifier classObj = new Classifier();
		classObj.setAcclerationVal();
		classObj.setRunAccVal();
		classObj.setWalkingacclerationVal();

		runAvg = classObj.computeAvg(classObj.runAccVal);
		runStdDev = classObj.computeDiffSq(classObj.runAccVal, runAvg);
		runPToP = classObj.computePeakToPeak(classObj.runAccVal);

		walkAvg = classObj.computeAvg(classObj.walkAccVal);
		walkStdDev = classObj.computeDiffSq(classObj.walkAccVal, walkAvg);
		walkPToP = classObj.computePeakToPeak(classObj.walkAccVal);

		standAvg = classObj.computeAvg(classObj.standAccVal);
		standStdDev = classObj.computeDiffSq(classObj.standAccVal, standAvg);
		standPToP = classObj.computePeakToPeak(classObj.standAccVal);

		//Load the sampled Data
		Utility.retrieveData(STAND_URL, standAcValLst);
		Utility.retrieveData(WALK_URL, walkAcValLst);
		Utility.retrieveData(RUN_URL, runAcValLst);

		runDataAvg = classObj.computeAvg(runAcValLst);
		runDataStdDev = classObj.computeDiffSq(runAcValLst, runDataAvg);
		runDataPeekToPeek = classObj.computePeakToPeak(runAcValLst);

		walkDataAvg = classObj.computeAvg(walkAcValLst);
		walkDataStdDev = classObj.computeDiffSq(walkAcValLst, walkDataAvg);
		walkDataPeekToPeek = classObj.computePeakToPeak(walkAcValLst);

		standDataAvg = classObj.computeAvg(standAcValLst);
		standDataStdDev = classObj.computeDiffSq(standAcValLst, standDataAvg);
		standDataPeekToPeek = classObj.computePeakToPeak(walkAcValLst);	
		
		//Create an ActivityFeature
		ActivityFeature runAct = new ActivityFeature();
		runAct.setAvg(runAvg);
		runAct.setMaxAvg(runAvg*2);
		runAct.setMinAvg(runAvg/2);
		runAct.setPeekToPeek(runPToP);
		runAct.setMaxPeekToPeek(runPToP*2);
		runAct.setMinPeekToPeek(runPToP/2);
		runAct.setStdDev(runStdDev);
		runAct.setMaxStdDev(runStdDev*2);
		runAct.setMinStdDev(runStdDev/2);

		//Create a DataSet Object
		DataSet data = new DataSet();
		data.setAvg(runDataAvg);
		data.setStdDev(runDataStdDev);
		data.setPeekToPeek(runDataPeekToPeek);
		double distRun = classObj.kNNClassifyDistance(runAct, data);
		//double distWalk = classObj.kNNClassifyDistance(walkAct, data);
		//double distStand = classObj.kNNClassifyDistance(standAct, data);
		//Should return "Run"
		
		//String act = classObj.kNNClassifyVoting(distRun, distWalk, distRun);
		
	}

	public double computePeakToPeak(ArrayList<Double> lst) {
		double peakToPeakVal = 0.0;
		double rmsVal = 0.0;
		double sumSqVal = 0.0;
		for (Iterator<Double> iterator = lst.iterator(); iterator.hasNext();) {
			Double item = (Double) iterator.next();
			double itemSqVal = Math.pow(item.doubleValue(), 2);
			sumSqVal+= itemSqVal;
		}
		rmsVal = Math.sqrt(sumSqVal/lst.size());
		// peak to peak = 2*Sqrt(2)*rmsVal
		peakToPeakVal = 2.8 * rmsVal;

		return peakToPeakVal;
	}

	public double computeDiffSq(ArrayList<Double> lst, double avgVal) {
		double stddev = 0.0;
		double sumVal = 0.0;
		for (Iterator<Double> iterator = lst.iterator(); iterator.hasNext();) {
			Double item = (Double) iterator.next();
			double itemVal = item.doubleValue();
			double sqVal = Math.pow((itemVal - avgVal), 2);
			sumVal += sqVal;
		}
		stddev = Math.sqrt(sumVal / lst.size());

		return stddev;
	}

	public double computeAvg(ArrayList<Double> lstVals) {
		double sum = 0.0;
		double avg = 0.0;
		double length = lstVals.size();
		for (Iterator<Double> iterator = lstVals.iterator(); iterator.hasNext();) {
			Double val = (Double) iterator.next();
			sum += val.doubleValue();
		}
		avg = sum / length;
		return avg;
	}

	public double kNNClassifyDistance(ActivityFeature act, DataSet query) {
		double avgDist, stdDevDist, diffSqDist, peekToPeekDist, globalDist;

		//Calculating Global Distance
		//using Manhattan distance

		//Local Distance of Avg
		avgDist = Math.abs(act.getAvg() - query.getAvg() / 
				(act.getMaxAvg() - act.getMinAvg()));

		//Local Distance of StdDev
		stdDevDist = Math.abs(act.getStdDev() - query.getStdDev() /
				(act.getMaxAvg() - act.getMinAvg()));

		//Local Distance of DiffSq
		//TODO

		//Local Distance of PeekToPeek
		peekToPeekDist = Math.abs(act.getPeekToPeek() - query.getPeekToPeek() /
				(act.getMaxPeekToPeek() - act.getMinPeekToPeek()));

		//TODO use weighted values if needed
		globalDist = avgDist + stdDevDist + peekToPeekDist;

		return globalDist;
	}

	/*
	 * Voting: 
	 * 
	 * We don't need voting for now. 
	 * 
	 */
	public String kNNClassifyVoting(double distance) {
		return null;
	}

}
