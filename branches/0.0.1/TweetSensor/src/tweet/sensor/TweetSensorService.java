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

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.widget.Toast;

/*
 * 
 * TweetSensorService Provides sensor events for accelerometer, location
 * Bind to the service class using bindService
 * Start the sensor signalling by sending a message
 * 
 * @author Ebby Wiselyn	<ebbywiselyn@gmail.com>
 * @author PraveenKumar Bhadrapur <praveenkumar.bhadarpur@gmail.com>
 * @version 0.0.1
 * 
 */
public class TweetSensorService extends Service {

	public static final int MSG_START_SENSING = 1;
	public static final int MSG_STOP_SENSING = 2;
	// Time in milliseconds
	public static final int LOCATION_MIN_TIME_CHANGE_UPDATE = 1000;
	// In meters
	public static final int LOCATION_MIN_DISTANCE_CHANGE_UPDATE = 1;
	
	private static boolean sHandlerRegistered = false;	
	private SensorManager mSensorMgr;
	private LocationManager mLocationMgr;

	class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

			case MSG_START_SENSING:
				mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
				mSensorMgr.registerListener(UserInfoHandler.getHandler(),
						mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_NORMAL);

				mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
				mLocationMgr.requestLocationUpdates(
						LocationManager.GPS_PROVIDER,
						LOCATION_MIN_TIME_CHANGE_UPDATE,
						LOCATION_MIN_DISTANCE_CHANGE_UPDATE,
						UserInfoHandler.getHandler());

				sHandlerRegistered = true;
				break;

			case MSG_STOP_SENSING:
				if (sHandlerRegistered) {
					mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
					mSensorMgr.unregisterListener(UserInfoHandler.getHandler());
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	final Messenger mMessenger = new Messenger(new IncomingHandler());
	static final String tag = "TweetSensor";

	@Override
	public IBinder onBind(Intent intent) {
		Toast.makeText(getApplicationContext(), "binding to sensor service", Toast.LENGTH_SHORT)
				.show();

		UserInfoHandler.getHandler().setService(this);
		return mMessenger.getBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Toast.makeText(getApplicationContext(), "unbinding sensor service", Toast.LENGTH_SHORT)
				.show();
		
		mSensorMgr.unregisterListener(UserInfoHandler.getHandler());
		UserInfoHandler.getHandler().setService(null);
		UserInfoHandler.clearHandler();
		return false;
	}
}
