package com.example;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

public class RandomTweetGenerator extends Service {

	private static final String TAG = "RandomTweetGenerator";
	// Handler hand;

	TweetDbActions dbActions;
	TweetContextActions contextActions;
	Handler hand;
	ConnectionHelper connHelper;
	SharedPreferences mSettings, prefs;
	static final int FALSE = 0;
	static final int TRUE = 1;
	private BluetoothAdapter mBtAdapter;
	static FileWriter generatorWriter;
	GenerateRandomTweets generateRandTweets = null;
	WakeLock wakeLock;
	String mac;

	@Override
	public void onCreate() {

		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);
		String user = mSettings.getString("user", "");
		dbActions = new TweetDbActions();
		hand = new Handler();
		generateRandTweets = new GenerateRandomTweets();
		hand.post(generateRandTweets);
		PowerManager mgr = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
		wakeLock.acquire();
		LogFilesOperations logOps = new LogFilesOperations();
		logOps.createLogsFolder();
		generatorWriter = logOps.createLogFile("TweetsGenerated_" + user);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mac = mBtAdapter.getAddress();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "on destroy");
		hand.removeCallbacks(generateRandTweets);
		wakeLock.release();
		try {
			generatorWriter.close();
		} catch (IOException e) {
		}
		super.onDestroy();
	}

	class GenerateRandomTweets implements Runnable {

		String user = mSettings.getString("user", "");

		public void run() {
			Log.i(TAG, "generating random tweets");
			long tweetsNumber = Math.round(Math.random()) + 1;

			for (int i = 0; i < tweetsNumber; i++) {
				String status = "random tweet "
						+ Math.round(Math.random() * 10000) + " " + user;
				long time = new Date().getTime();
				if (dbActions.saveIntoDisasterDb(status.hashCode(), time, time,
						status, user, "", FALSE, TRUE, TRUE, 0)) {
					try {
						generatorWriter.write(mac + ":" + user + ":"
								+ status.hashCode() + ":" + time + ":"
								+ new Date().toString() + "\n");
					} catch (IOException e) {
					}
					sendBroadcast(new Intent(Timeline.ACTION_NEW_DISASTER_TWEET));
					// dbActions.copyIntoTimelineTable(status.hashCode(),time,
					// status,user,FALSE);
				} else
					Log.i(TAG, "tweet not added to the disaster table");
			}
			long delay = Math.round(Math.random() * 240000) + 840000;
			hand.postDelayed(this, delay);
		}
	}

}
