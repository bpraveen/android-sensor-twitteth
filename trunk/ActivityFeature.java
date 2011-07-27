package com.example;
/* 
 * Classification of Activities
 * 
 * Standing, Walking, Running
 * 
 */
public class ActivityFeature {
	public enum Name {
		ACTIVITY_RUN,
		ACTIVITY_WALK,
		ACTIVITY_STAND,
		ACTIVITY_UNKNOWN
	};
	
	private Name name = Name.ACTIVITY_UNKNOWN;
	
	/* Standard Deviation */
	private double stdDev;
	private double maxStdDev;
	private double minStdDev;
	
	/* Peek to Peek */
	private double peekToPeek;
	private double minPeekToPeek;
	private double maxPeekToPeek;
	
	/* Average */
	private double avg;
	private double minAvg;
	private double maxAvg;
	
	//TODO
	/* 
	 * Wave Frequency 
	 * Got by doing a fourier transform of the signal
	 * on the sample window. 
	 * 
	 * NOTE: The window size should 512 to make it 2^n
	 * for easier fourier transformation. 
	 * 
	 * 
	 */
	
	//private double freq;
	//private double minFreq;
	//private double maxFreq;
	
	public double getAvg() {
		return avg;
	}
	
	public void setAvg(double avg) {
		this.avg = avg;
	}
	
	public double getStdDev() {
		return this.stdDev;
	}
	
	public void setStdDev(double stdDev) {
		this.stdDev = stdDev;
	}
	
	public void setPeekToPeek(double peekToPeek) {
		this.peekToPeek = peekToPeek;
	}
	
	public double getPeekToPeek() {
		return this.peekToPeek;
	}
	
	public double getMinPeekToPeek() {
		return this.minPeekToPeek;
	}
	
	public void setMinPeekToPeek(double minPeekToPeek) {
		this.minPeekToPeek = minPeekToPeek;
	}
	
	public double getMaxPeekToPeek() {
		return this.maxPeekToPeek;
	}
	
	public void setMaxPeekToPeek(double maxPeekToPeek) {
		this.maxPeekToPeek = maxPeekToPeek;
	}
	
	public double getMinStdDev() {
		return this.minStdDev;
	}
	
	public void setMinStdDev(double minStdDev) {
		this.minStdDev = minStdDev;
	}
	
	public void setMaxStdDev(double maxStdDev) {
		this.maxStdDev = maxStdDev;
	}
	
	
	
	public double getMinAvg() {
		return this.minAvg;
	}
	
	public double getMaxAvg() {
		return this.maxAvg;
	}
	
	public void setMinAvg(double minAvg) {
		this.minAvg = minAvg;
	}
	
	public void setMaxAvg(double maxAvg) {
		this.maxAvg = maxAvg;
	}

	public void setName(Name obj) {
		this.name = obj;
	}
	
	public String getName(Name obj) {
		switch(obj) {
			case ACTIVITY_RUN:
				return "running";
			case ACTIVITY_WALK:
				return "walking";
			case ACTIVITY_STAND:
				return "stand";
			default:
				return "unknown";
		}
		
	}

}

