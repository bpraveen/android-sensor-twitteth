package com.example;

/*
 * List of Attributes of a Data Instance
 * A Data Instance in our case will be a logged sample. 
 * A logged sample will have totalSample number of 
 * samples 
 */

public class DataSet {
	//Class
	String className;
	
	//Attributes
	private double stdDev;
	private double avg;
	private double peekToPeek;
	
	//Computed Result
	private double globalDist;
	
	private String insName; 
	
	public void setStdDev(double stdDev) {
		this.stdDev = stdDev;
	}
	
	public double getStdDev() {
		return stdDev;
	}
	
	public double getAvg() {
		return avg;
	}
	
	public void setAvg(double avg) {
		this.avg = avg;
	}
	
	public double getPeekToPeek() {
		return this.peekToPeek;
	}
	
	public void setPeekToPeek(double peekToPeek) {
		this.peekToPeek = peekToPeek;
	}
	
	public void setName(String name) {
		this.insName = name;
	}
	
	public String getName() {
		return this.insName;
	}

}
