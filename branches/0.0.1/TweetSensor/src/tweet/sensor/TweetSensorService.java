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

public class TweetSensorService extends Service {

	private static boolean handlerRegistered = false;
	private SensorManager mSensorMgr;
	private LocationManager mLocationMgr;

	public static final int MSG_START_SENSING = 1;
	public static final int MSG_STOP_SENSING = 2;

	// Time in milliseconds
	public static final int LOCATION_MIN_TIME_CHANGE_UPDATE = 1000;
	// In meters
	public static final int LOCATION_MIN_DISTANCE_CHANGE_UPDATE = 1;

	class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

			case MSG_START_SENSING:
				Toast.makeText(getApplicationContext(), "starting sensor",
						Toast.LENGTH_SHORT).show();

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

				handlerRegistered = true;
				break;

			case MSG_STOP_SENSING:
				if (handlerRegistered) {
					Toast.makeText(getApplicationContext(), "stopping sensor",
							Toast.LENGTH_SHORT).show();
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
		Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT)
				.show();

		UserInfoHandler.getHandler().setService(this);
		return mMessenger.getBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Toast.makeText(getApplicationContext(), "unbinding", Toast.LENGTH_SHORT)
				.show();

		UserInfoHandler.getHandler().setService(null);
		return false;
	}
}