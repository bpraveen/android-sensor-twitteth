package com.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {
	/** Called when the activity is first created. */

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Bundle extras = intent.getExtras();

		if (extras != null) {
			String userInfo = extras.getString("userInfoStr");
			Log.d("TweetSensor", "TweetSensor UserInfo" + userInfo);
		}

		Log.d("TweetSensor", "TweetSensor Activity Received");
	}
}
