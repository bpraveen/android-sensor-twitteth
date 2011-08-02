/*
 * 
 * Copyright (C) 2010 The Android Open Source Project 
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
 * 
 */

package tweet.sensor;

import java.util.LinkedList;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

/*
 * 
 * Class for Signal handling of sensor event listener, location listener
 * 
 * @author Ebby Wiselyn	<ebbywiselyn@gmail.com>
 * @author PraveenKumar Bhadrapur <praveenkumar.bhadarpur@gmail.com>
 * @version 0.0.1
 * 
 */
public class UserInfoHandler implements SensorEventListener, LocationListener {

	private int mSampleNo = 0;
	private int mTotalSamples = 256;
	private double mDurationSample = 256.0;
	private double mLongitude, mLatitude;
	private long mStartTime = System.currentTimeMillis();
	private long mSamplingTime[] = new long[mTotalSamples];
	private LinkedList<Number> mAccList = new LinkedList<Number>();
	private TweetSensorService mSensorService;

	// FIXME check if static is required
	public static String userInfoStr;
	private static UserInfoHandler sHandler;

	/*
	 * Acceleration from x, y, z accelerometer values
	 * 
	 * @param x acceleration in x axis
	 * 
	 * @param y acceleration in y axis
	 * 
	 * @param z acceleration in z axis
	 * 
	 * @return double acceleration
	 */
	private double getAcceleration(double x, double y, double z) {
		return Math.sqrt(x * x + y * y + z * z);
	}

	/*
	 * Singleton class implementation, Can create only one object
	 * 
	 * @return UserInfoHandler returns instance of UserInfoHandler
	 */
	public static UserInfoHandler getHandler() {
		if (sHandler == null) {
			sHandler = new UserInfoHandler();
		}

		return sHandler;
	}

	/*
	 * Set the service instance which creates the handler, Required for calling
	 * service specific methods like sending broadcast
	 * 
	 * @param sensorService service instance which creates the andler
	 * 
	 * @return void
	 */
	public void setService(TweetSensorService sensorService) {
		this.mSensorService = sensorService;
	}

	/*
	 * onAccuracyChanged Callback method for change in sensor accuracy
	 * 
	 * @param sensor Sensor
	 * 
	 * @param accuracy change in accuracy
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * onSensorChanged Callback method for change in sensor event
	 * 
	 * @param event SensorEvent which occurred
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		double acc, x, y, z;
		double linear_data[], smoothed_data[];
		String act;

		Log.d(TweetSensorService.tag, TweetSensorService.tag + "mSampleNo:"
				+ mSampleNo);

		x = event.values[0];
		y = event.values[1];
		z = event.values[2];

		mSamplingTime[mSampleNo] = System.currentTimeMillis() - mStartTime;
		acc = getAcceleration(x, y, z);
		mAccList.add(acc);

		// Process every mTotalSamples interval
		if (mSampleNo >= mTotalSamples - 1) {

			// Data Interpolation Linearization
			linear_data = UserInfoUtil.linearizeData(mDurationSample,
					mTotalSamples, mSamplingTime, mAccList);
			
			// Smoothen the data
			smoothed_data = UserInfoUtil.smoothenData(linear_data);
			
			// Classify Activity
			act = UserInfoActivityClassifier.kNNClassifyActivity(smoothed_data);
			userInfoStr = act + "longitude:" + mLongitude + "latitude:"
					+ mLatitude;
			
			Log.d(TweetSensorService.tag, TweetSensorService.tag + "_Activity"
					+ userInfoStr);

			// Broadcast the data
			if (mSensorService != null) {
				//TODO: Use appropriate action
				Intent i = new Intent();
				i.putExtra("userInfoStr", userInfoStr);
				i.setAction(Intent.ACTION_VIEW);
				mSensorService.sendBroadcast(i);
			}

			// Reset the start time
			mStartTime = System.currentTimeMillis();
		}

		mSampleNo = mSampleNo + 1;
		mSampleNo = mSampleNo % mTotalSamples;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.location.LocationListener#onLocationChanged(android.location.
	 * Location)
	 */
	@Override
	public void onLocationChanged(Location location) {
		mLatitude = location.getLatitude();
		mLongitude = location.getLongitude();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String,
	 * int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}
