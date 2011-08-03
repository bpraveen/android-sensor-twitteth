package com.example;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MyTwitter extends Activity  implements OnClickListener {

	static final String TAG = "MyTwitter";

	SharedPreferences mSettings;
	Button buttonOAuth;
	ConnectionHelper connHelper;
	private DbOpenHelper dbHelper;
	public String status;
	static MyTwitter activity;
	static PendingIntent restartIntent;
	
	public void onClick(View src) {
		switch (src.getId()) {
		case R.id.buttonOAuth:
			// finish();
			startActivity(new Intent(this, OAUTH.class));
			break;
		}
	}

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "inside on create");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (Timeline.isRunning) {
			finish();
			// Timeline activity handles the menu options for sending tweets
			startActivity(new Intent(this, Timeline.class));
		} else {
			mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);
			// find views by id
			buttonOAuth = (Button) findViewById(R.id.buttonOAuth);
			activity = this;
			restartIntent = PendingIntent.getActivity(this.getBaseContext(), 0,
					new Intent(getIntent()), getIntent().getFlags());
			ifTokensTryLogin();
			// Add listeners
			buttonOAuth.setOnClickListener(this);
		}

	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		ifTokensTryLogin();
	}

	// Called when menu item is selected //
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		System.out.println ("Menu options selected " + item.getItemId());
		switch (item.getItemId()) {
			
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
	}

	private void ifTokensTryLogin() {

		if (mSettings.contains(OAUTH.USER_TOKEN)
				&& mSettings.contains(OAUTH.USER_SECRET)) {
			ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			connHelper = new ConnectionHelper(mSettings, connec);
			startService(new Intent(this, UpdaterService.class));
			if (connHelper.testInternetConnectivity()) {
				if (connHelper.doLogin()) {
					Toast
							.makeText(this, "Login Successful",
									Toast.LENGTH_SHORT).show();
					// Start the UpdaterService
					startActivity(new Intent(this, Timeline.class));
					finish();
				} else {
					Toast.makeText(this, "Incorrect Login, showing old tweets",
							Toast.LENGTH_LONG).show();
					startActivity(new Intent(this, Timeline.class));
					finish();
				}
			} else {
				Toast.makeText(this, "No internet connectivity",
						Toast.LENGTH_SHORT).show();
				startActivity(new Intent(this, Timeline.class));
				finish();
			}
			finish();

		} else
			Toast.makeText(this,
					"Press the button above to authorize the client",
					Toast.LENGTH_LONG).show();

	}

}
