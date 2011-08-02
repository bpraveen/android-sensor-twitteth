package com.example;

import java.io.IOException;
import java.util.Date;

import winterwell.jtwitter.Twitter.Status;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SendMessageWrapper extends Activity {
	private String statusMessage = "MyTweet3";
	private EditText input;
	ConnectionHelper connHelper;
	SharedPreferences mSettings, prefs;
	static ConnectivityManager connec;
	TweetDbActions dbActions;
	static final int FALSE = 0;
	int hasBeenSent = FALSE;
	static final int TRUE = 1;
	BluetoothAdapter mBtAdapter;
	private Cursor cursor, cursorDisaster;

	static final String ACTION_NEW_DISASTER_TWEET = "New disaster tweet";

	private static final String TAG = SendMessageWrapper.class.toString();

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "inside on create");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sendmessage);

		mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);
		connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		connHelper = new ConnectionHelper(mSettings, connec);
		dbActions = new TweetDbActions();

		Button sendButton = (Button) findViewById(R.id.button);
		sendButton.setOnClickListener(new ClickListener());
	}

	private class ClickListener implements OnClickListener {
		public void onClick(View v) {
			sendMessage(statusMessage);
		}
	}

	void sendMessage(String status) {
		//boolean isDisaster = prefs.getBoolean("prefDisasterMode", false);
		// send a msg if there is connectivity and length is ok (or if we are in
		// disaster mode)
		if (connHelper.testInternetConnectivity() ) {
			if (status.length() > 0 && status.length() <= 140) { // if length is
				// ok
				new SendTask(0, status, false).execute(); // try to send it
			} else if (status.length() > 140) {
				Toast.makeText(this, "The message is too long ",
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, "No text to be sent ", Toast.LENGTH_LONG)
						.show();
			}
		} else {
			Toast.makeText(this, "No Internet connection", Toast.LENGTH_LONG)
					.show();
		}
	}

	class SendTask extends AsyncTask<Void, Void, Boolean> {
		long id;
		String status;
		ProgressDialog postDialog;
		boolean isDisaster;

		public SendTask(long id, String status, boolean isDisaster) {
			this.id = id;
			this.status = status;
			this.isDisaster = isDisaster;
		}

		@Override
		protected void onPreExecute() {
			postDialog = ProgressDialog.show(SendMessageWrapper.this,
					"Sending message",
					"Please wait while your message is being sent", true, // indeterminate
					// duration
					false); // not cancel-able
		}

		@Override
		protected Boolean doInBackground(Void... message) {
			return setStatus(id, status);
		}

		// This is in the UI thread, so we can mess with the UI
		@Override
		protected void onPostExecute(Boolean result) {
			postDialog.dismiss();
			if (result) { // if it has been sent correctly
				input.setText(""); // reset text
				if (id == 0) { // if is just a normal sending and not publishing
					// all disaster tweets
					// if (connHelper.testInternetConnectivity())
					// new RefreshTimeline(false).execute();
					Toast.makeText(SendMessageWrapper.this,
							"Tweet has been sent", Toast.LENGTH_SHORT).show();
					hasBeenSent = TRUE;
				}
			} else { // if it hasnt been sent
				if (id == 0) // and it is a normal sending
					Toast.makeText(SendMessageWrapper.this, "Tweet not sent",
							Toast.LENGTH_SHORT).show();
			}

			// if (isDisaster == true) { // if we are in disaster mode
			// if (status.length() > 0 && status.length() <= 140) { // and
			// // length
			// // is ok
			// String user = mSettings.getString("user", "not found");
			// String concatenate = status.concat(" " + user);
			// if (dbActions.saveIntoDisasterDb(concatenate.hashCode(),
			// new Date().getTime(), new Date().getTime(), status,
			// user, "", FALSE, hasBeenSent, TRUE, 0)) {
			// String mac = mBtAdapter.getAddress();
			// long time = new Date().getTime();
			// try {
			// if (RandomTweetGenerator.generatorWriter != null)
			// RandomTweetGenerator.generatorWriter.write(mac
			// + ":" + user + ":" + status.hashCode()
			// + ":manual:" + time + ":"
			// + new Date().toString() + "\n");
			// } catch (IOException e) {
			// }
			// // if it has been added successfully
			// sendBroadcast(new Intent(ACTION_NEW_DISASTER_TWEET));
			// if (!result) { // if it has not been successfully I need
			// // to show in the timeline
			// // otherwise it means i will receive it from online
			// // central servers
			// dbActions.copyIntoTimelineTable(concatenate
			// .hashCode(), new Date().getTime(), status,
			// user, FALSE);
			// cursor.requery();
			// }
			// }
			// hasBeenSent = FALSE;
			//
			// }
			// input.setText("");
			// }
		}
	}

	private boolean setStatus(long id, String status) {// I need an id since
		// it
		// is used for publish
		// disaster tweets as
		// well
		Log.i(TAG, " Setting Status " + status);
		if (ConnectionHelper.twitter == null) {
			System.out.println(" Time Line setStatus login");
			connHelper.doLogin();
		}
		try {
			if (ConnectionHelper.twitter != null) {
				Status d = ConnectionHelper.twitter.setStatus(status);

				Log.i(TAG, "tweet published" + d.text);
				if (id != 0) { // in case we are automatically publishing
					// disaster tweets
					// dbActions.updateDisasterTable(id, TRUE, TRUE);
					// dbActions.delete(DbOpenHelper.C_ID + "=" + id,
					// DbOpenHelper.TABLE);
				}
				return true;
			} else {
				Log.i(TAG, "ConnectionHelper.twitter null , returned false");
				return false;
			}
		} catch (Exception ex) {
			Log.i(TAG, "tweet not published");
			return false;
		}
	}
}
