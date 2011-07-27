package com.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYPlot.BoundaryMode;


public class AccDemo extends Activity implements SensorEventListener {
	/*
	 * xyzPlot XYPlot of x, y, z accelerometer values
	 * accPlot XYPlot of acceleration value
	 * acc = sqrt(x^2 + y^2 + z^2)
	 *
	 */
	private SensorManager sensorMgr;
	private XYPlot xyzPlot;
	private XYPlot accPlot;
	private XYPlot linPlot;
	private XYPlot smoothPlot;

	private SimpleXYSeries rawX;
	private SimpleXYSeries rawY;
	private SimpleXYSeries rawZ;
	private SimpleXYSeries acc;
	private SimpleXYSeries lin;
	private SimpleXYSeries smooth;

	private LinkedList<Number> xList;
	private LinkedList<Number> yList;
	private LinkedList<Number> zList;
	private LinkedList<Number> accList;
	private LinkedList<Number> linList;
	private LinkedList<Number> smoothList;

	private static boolean buffer_available = true;
	private static boolean logEnabled = false;
	private static double SAMPLE_DUR = 250.0;
	private static long startReadT = 0;

	private int totalSample = 200;
	private long[] samplingTime = new long[totalSample];
	private static int incInt = 0;
	private static int logNum = 0;

	//Block Initialization
	{
		xList = new LinkedList<Number>();
		yList = new LinkedList<Number>();
		zList = new LinkedList<Number>();
		accList = new LinkedList<Number>();
		linList = new LinkedList<Number>();
		smoothList = new LinkedList<Number>();

		rawX = new SimpleXYSeries("rawX");
		rawY = new SimpleXYSeries("rawY");
		rawZ = new SimpleXYSeries("rawZ");
		acc = new SimpleXYSeries("accXY");
		lin = new SimpleXYSeries("linXY");
		smooth = new SimpleXYSeries("smoXY");
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acc);

		xyzPlot = (XYPlot)findViewById(R.id.rawLevelsPlot);
		xyzPlot.addSeries(rawX, LineAndPointRenderer.class, new LineAndPointFormatter(Color.rgb(100, 100, 200), Color.BLACK));
		xyzPlot.addSeries(rawY, LineAndPointRenderer.class, new LineAndPointFormatter(Color.rgb(100, 200, 100), Color.BLACK));
		xyzPlot.addSeries(rawZ, LineAndPointRenderer.class, new LineAndPointFormatter(Color.rgb(200, 100, 100), Color.BLACK));
		xyzPlot.setDomainBoundaries(0, totalSample, XYPlot.BoundaryMode.FIXED);
		xyzPlot.setRangeBoundaries(-5, 5, BoundaryMode.FIXED);
		xyzPlot.setDomainStepValue(3);
		xyzPlot.setTicksPerRangeLabel(3);
		xyzPlot.setRangeLabel("x, y, z");
		xyzPlot.getRangeLabelWidget().pack();
		xyzPlot.setDomainLabel("milli meter");
		xyzPlot.getDomainLabelWidget().pack();
		xyzPlot.disableAllMarkup();

		accPlot = (XYPlot)findViewById(R.id.accLevelsPlot);
		accPlot.addSeries(acc, LineAndPointRenderer.class, new LineAndPointFormatter(Color.rgb(200, 100, 100), Color.BLACK));
		accPlot.setDomainStepValue(6);
		accPlot.setTicksPerRangeLabel(3);
		accPlot.setDomainBoundaries(0, totalSample, XYPlot.BoundaryMode.FIXED);
		accPlot.setRangeBoundaries(7, 10, BoundaryMode.FIXED);
		accPlot.setDomainLabel("time");
		accPlot.setRangeLabel("acc mm");
		accPlot.getDomainLabelWidget().pack();
		accPlot.disableAllMarkup();

		linPlot = (XYPlot)findViewById(R.id.linLevelsPlot);
		linPlot.addSeries(lin, LineAndPointRenderer.class, new LineAndPointFormatter(Color.rgb(200, 100, 100), Color.BLACK));
		linPlot.addSeries(smooth, LineAndPointRenderer.class, new LineAndPointFormatter(Color.rgb(100, 200, 100), Color.BLACK));
		linPlot.setDomainStepValue(6);
		linPlot.setTicksPerRangeLabel(3);
		linPlot.setDomainLabel("time");
		linPlot.setRangeLabel("acc mm");
		linPlot.setRangeBoundaries(7, 10, BoundaryMode.FIXED);
		linPlot.getDomainLabelWidget().pack();
		linPlot.disableAllMarkup();

		/*
		smoothPlot = (XYPlot)findViewById(R.id.smoothLevelsPlot);
		smoothPlot.addSeries(smooth, LineAndPointFormatter.class, new LineAndPointFormatter(Color.rgb(200, 100, 100), Color.BLACK));
		smoothPlot.setDomainLabel("time");
		smoothPlot.setRangeLabel("acc mm");
		smoothPlot.setRangeBoundaries(7, 10, BoundaryMode.FIXED);
		smoothPlot.disableAllMarkup();
		 */

		//Register for accelerometer sensor events
		sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorMgr.registerListener(this,
				sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);

		startReadT = System.currentTimeMillis();
	}



	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {

	}


	public int boundl_index(int x, long[] values) {
		int len = values.length;

		if ((values[0] > x) || (values[0] == x)) {
			return 0;
		} 

		for (int i=1; i < len-1; i++) {
			if ((values[i] > x) || (values[i] == x)) {
				return i-1;
			}
		}

		return len-1;		
	}

	public int boundr_index(int x, long[] values) {
		int len = values.length;

		if ((values[0] > x) || (values[0] == x)) {
			return 0;
		}

		for (int i=1; i < len-1; i++) {
			if ((values[i] > x) || (values[i] == x)) {
				return i;
			}
		}

		return len-1;
	}

	/*
	 * 5 point smoothening algorithm
	 */
	public double[] smoothen(double[] v) {
		int size = v.length-1;
		System.out.println ("size:" + size);
		double[] smooth = new double[size+1];

		smooth[0] = v[0];
		smooth[1] = v[1];

		for (int i=2; i<size-2; i++) {
			smooth[i] = (v[i-2] + v[i-1] + v[i] + v[i+1] + v[i+2]) / 5.0;
		}

		smooth[size-1] = v[size-1];
		smooth[size] = v[size];


		return smooth;
	}


	public double[] linearize(LinkedList<Number> accList, long[] samplingTime) {
		int index = 0;
		int dur = (int)SAMPLE_DUR;
		double x0_msec, x1_msec, x, x0, x1, y, y0=0, y1=0;
		double linear[] = new double[totalSample];

		//Special case first index
		linear[0] = 0;

		//Regular case
		for (int time = dur; time < totalSample*dur; time = time + dur) {
			index = time / dur;
			index = index % totalSample;

			int posl = boundl_index(time, samplingTime);
			int posr = boundr_index(time, samplingTime);

			x0_msec = samplingTime[posl];
			x1_msec = samplingTime[posr];

			x0 = x0_msec / SAMPLE_DUR;
			x1 = x1_msec / SAMPLE_DUR;	

			y0 = accList.get(posl).doubleValue();
			y1 = accList.get(posr).doubleValue();
			x = index;

			//Calculate y
			System.out.println ("x0"+x0+"x1,"+x1+",y0"+y0+",y1"+y1);
			linear[index] = y0 + (y1 - y0)*(x-x0)/(x1-x0);
			System.out.println ("diff:" + (linear[index] - accList.get(index).doubleValue()));
		}

		return linear;
	}



	@Override
	public void onSensorChanged(SensorEvent event) {

		samplingTime[incInt] = System.currentTimeMillis() - startReadT;
		System.out.println ("interval in msec :" + samplingTime[incInt]);

		if (xList.size() > totalSample) {
			xList.removeFirst();
			yList.removeFirst();
			zList.removeFirst();
			accList.removeFirst();
		}

		xList.addLast(event.values[0]);
		yList.addLast(event.values[1]);
		zList.addLast(event.values[2]);
		accList.addLast(acceleration(event.values[0], event.values[1], event.values[2]));

		//add acc ever totalSample interval
		if (incInt >= (totalSample-1)) {
			System.out.println ("interval window end time:" + (System.currentTimeMillis() - startReadT));

			double linear[] = new double[totalSample];
			linList = new LinkedList<Number>();

			linear = linearize(accList, samplingTime);
			for (double d : linear) {
				linList.addLast(d);
			}

			double smooth_values[] = smoothen(linear);
			for (double d : smooth_values) {
				smoothList.addLast(d);
			}

			acc.setModel(accList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
			lin.setModel(linList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
			smooth.setModel(smoothList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

			//Log smooth x, y value
			if (logEnabled) {
				for (int x=0; x<smoothList.size(); x++) {
					appendLog("["+x+","+smoothList.get(x)+"] ", logNum);
				}
			}

			linPlot.redraw();
			accPlot.redraw();

			linList = new LinkedList<Number>();
			smoothList = new LinkedList<Number>();

			//Reset start time end of window frame
			startReadT = System.currentTimeMillis();
		}

		rawX.setModel(xList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
		rawY.setModel(yList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
		rawZ.setModel(zList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
		xyzPlot.redraw();

		incInt = incInt + 1;
		incInt = incInt % totalSample;		
	}

	private double acceleration(double x, double y, double z) {
		return Math.sqrt(x*x+y*y+z*z);
	}

	private class AccProcess extends Thread {

		public void run() {
			//Process data 
			for (long l : samplingTime) {
				System.out.println ("startTime " + l);
			}

			//Initialise linear Plot

			//Initialise smooth Plot			

			//Last statement
			buffer_available = true;
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 1, 0, "unregister");
		menu.add(0, 2, 1, "Log Enabled");

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			if (item.getTitle().equals("unregister")) {
				sensorMgr.unregisterListener(this);

				item.setTitle("Register");
			} else {
				sensorMgr.registerListener(this, 
						sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
						SensorManager.SENSOR_DELAY_NORMAL);

				item.setTitle("UnRegister");	    		
			}
			return true;
		case 2:
			if(logEnabled == true) {
				logEnabled = false;
				item.setTitle("Enable Logging");
				return true;
			}
			else { 
				logEnabled = true;
				item.setTitle("Disable Logging");
				return true;
			}
		default:
			sensorMgr.unregisterListener(this);
			return super.onOptionsItemSelected(item);
		}		
	}

	public void appendLog(String text, int no) {
		File logFile = new File("sdcard/sensorLog_" + no + "_.file");
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}

		try {
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
			buf.append(text);
			buf.newLine();
			buf.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Implementation of k-NN Algorithm
	 * 
	 */
	public String onlineClassify () {
		return null;
	}

}
