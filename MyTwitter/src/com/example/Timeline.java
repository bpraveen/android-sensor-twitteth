package com.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/** Displays the list of all timelines from the DB. */
public class Timeline extends Activity {

	static final String TAG = "Timeline";
	private ListView listTimeline;
	private EditText input;
	// Handler handler;
	boolean peerRequestedClosing = false;

	private SQLiteDatabase db;
	private DbOpenHelper dbHelper;
	private Cursor cursor, cursorDisaster;
	private TimelineAdapter adapter;
	TweetDbActions dbActions;
	TweetContextActions contextActions;
	ArrayList<Status> results = null;
	AlertDialog.Builder alert;
	ConnectionHelper connHelper;
	public String mToken;
	public String mSecret;
	BluetoothAdapter mBtAdapter;
	NotificationManager notificationManager;

	SharedPreferences mSettings, prefs;
	public static boolean isRunning = false;
	int hasBeenSent = FALSE;
	private boolean isShowingFavorites = false;

	static final String ACTION_NEW_DISASTER_TWEET = "New disaster tweet";
	private static final int PREF_SCREEN = 1;

	static final int REPLY_ID = Menu.FIRST;
	static final int RETWEET_ID = Menu.FIRST + 1;
	static final int DELETE_ID = Menu.FIRST + 2;
	static final int FAVORITE_ID = Menu.FIRST + 3;
	static final int R_FAVORITE_ID = Menu.FIRST + 4;

	static final int REFRESH_ID = Menu.FIRST;
	static final int FRIENDS_ID = Menu.FIRST + 1;
	static final int SEND_ID = Menu.FIRST + 2;
	static final int SETTINGS_ID = Menu.FIRST + 3;
	static final int DISASTER_ID = Menu.FIRST + 4;
	static final int EXIT_ID = Menu.FIRST + 5;
	static final int FAVORITES_ID = Menu.FIRST + 6;
	static final int TIMELINE_ID = Menu.FIRST + 7;
	static final int MENTIONS_ID = Menu.FIRST + 8;
	static final int PUBLISH_ID = Menu.FIRST + 9;
	static final int LOGOUT_ID = Menu.FIRST + 10;

	static final int FALSE = 0;
	static final int TRUE = 1;
	static final int NOTIFICATION_ID = 47;
	static final long DELAY = 10000L;

	static PendingIntent restartIntent;
	static Timeline activity;
	WakeLock wakeLock;
	static ConnectivityManager connec;
	String userName = "";
    Messenger mService = null;
	boolean mBound;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simplelist);
		isRunning = true;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		connHelper = new ConnectionHelper(mSettings, connec);

		input = new EditText(this);
		// Find views by id
		listTimeline = (ListView) findViewById(R.id.itemList);
		dbActions = new TweetDbActions();
		contextActions = new TweetContextActions(connHelper, prefs);
		dbHelper = new DbOpenHelper(this);
		db = dbHelper.getWritableDatabase();

		registerForContextMenu(listTimeline);
		// Register to get ACTION_NEW_TWITTER_STATUS broadcasts
		registerReceiver(twitterStatusReceiver, new IntentFilter(
				UpdaterService.ACTION_NEW_TWITTER_STATUS));		
		
		activity = this;
		// delete really old tweets
		String where = DbOpenHelper.C_CREATED_AT + "<"
				+ (new Date().getTime() - (3600000 * 24 * 2));
		dbActions.delete(where, DbOpenHelper.TABLE);

		where = DbOpenHelper.C_CREATED_AT + "<"
				+ (new Date().getTime() - 86400000 / 2);
		dbActions.delete(where, DbOpenHelper.TABLE_DISASTER);

		// Get the data from the DB
		cursor = db.query(DbOpenHelper.TABLE, null, null, null, null, null,
				DbOpenHelper.C_CREATED_AT + " DESC", "100");
		Cursor cursorPictures = db.query(DbOpenHelper.TABLE_PICTURES, null,
				null, null, null, null, null);
		cursorPictures.moveToFirst();
		// Setup the adapter
		adapter = new TimelineAdapter(this, cursor, cursorPictures);
		listTimeline.setAdapter(adapter);

		if (prefs.getBoolean("prefDisasterMode", false) == true) {
			// it means we are restarting from a crash
			startServices();
		}
		new GetAuthenticatingUsername().execute();

	}

	@Override
	protected Dialog onCreateDialog(int id) {

		Log.i(TAG, "onCreateDialog : sending tweets ");
		alert = new AlertDialog.Builder(this);
		alert.setTitle("Send a Tweet");

		// Set an EditText view to get user input
		alert.setView(input);
		alert.setPositiveButton("Send", new OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String message = input.getText().toString();
				sendMessage(message);
				dialog.dismiss();
			}
		});

		alert.setNegativeButton("Cancel", new OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				input.setText("");
				dialog.dismiss();
			}
		});
		return alert.create();
	}

	private void cancelNotification() {
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(UpdaterService.Updater.NOTIFICATION_ID);
		if (isShowingFavorites)
			changeView(false, DbOpenHelper.TABLE);
	}

	@Override
	protected void onStart() {
		super.onStart();

		publishDisasterTweets(false);
	}

	private class DeleteMyTweets implements Runnable {

		public void run() {
			try {
				if (ConnectionHelper.twitter != null) {
					try {
						ConnectionHelper.twitter.setCount(100);
					} catch (Exception e) {
					}

					ArrayList<Twitter.Status> tweets = (ArrayList<Twitter.Status>) ConnectionHelper.twitter
							.getUserTimeline();
					for (Twitter.Status status : tweets) {
						contextActions.deleteTweet(status.getId().longValue(),
								false);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
					}
				}
			} catch (TwitterException ex) {
			}
		}
	}

	class GetAuthenticatingUsername extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... nil) {
			try {
				if (connHelper.testInternetConnectivity()) {
					if (ConnectionHelper.twitter != null) {
						Twitter.Status status = ConnectionHelper.twitter
								.getStatus();
						if (status == null) {
							ConnectionHelper.twitter.setStatus(".");
							try {
								Thread.sleep(1000);
							} catch (InterruptedException ex) {
							}
							status = ConnectionHelper.twitter.getStatus();
							if (status != null) {
								userName = status.getUser().getScreenName();
								Long id = status.getId().longValue();
								ConnectionHelper.twitter.destroyStatus(id);
								return userName;
							} else
								return "";
						} else {
							userName = status.getUser().getScreenName();
							return userName;
						}
					} else
						return "";
				} else
					return "";

			} catch (Exception ex) {
				Log.i(TAG, "getting username exception");
				return "";
			}
		}

		// This is in the UI thread, so we can mess with the UI
		@Override
		protected void onPostExecute(String result) {
			if (!result.equals("")) {
				SharedPreferences.Editor editor = mSettings.edit();
				editor.remove("user");
				editor.putString("user", result);
				editor.commit();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "inside on resume");
		// Cancel notification
		cancelNotification();

		// new Thread(new DeleteMyTweets()).start();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "inside on destroy, stopping everything");
		unregisterReceiver(twitterStatusReceiver);
		unregisterReceiver(sensorDataReceiver);
		isRunning = false;
		stopService(new Intent(this, UpdaterService.class));
		unbindService(mConnection);
		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		mBtAdapter.disable();
		if (prefs.getBoolean("prefDisasterMode", false))
			stopService(new Intent(this, DisasterOperations.class));
		// disable disaster mode
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("prefDisasterMode", false);
		editor.commit();
		publishDisasterTweets(true);
		cursor.close();
		db.close();
		ConnectionHelper.twitter = null;
		super.onDestroy();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Cursor cursor;

		if (isShowingFavorites == false) { // I am not showing favorites
			cursor = dbActions.disasterDbQuery(DbOpenHelper.C_ID + "="
					+ info.id);
			String tweetUser = dbActions.userDbQuery(info, DbOpenHelper.TABLE);
			if (cursor != null) {
				// if the tweet is not a disaster one i can add it as a favorite
				if (cursor.getCount() == 0
						&& !tweetUser.equals(mSettings.getString("user", ""))) {
					// show favorite if it isn t a disaster tweet and I am not
					// the author
					menu.add(0, FAVORITE_ID, 3, "Favorite");
				}
			}
			if (tweetUser.equals(mSettings.getString("user", ""))) {
				menu.add(0, DELETE_ID, 2, "Delete");
			} else {
				menu.add(0, RETWEET_ID, 1, "Retweet");
				menu.add(0, REPLY_ID, 0, "Reply");
			}
		}

		// is showing the favorite tweets
		else {
			menu.add(0, REPLY_ID, 0, "Reply");
			menu.add(0, R_FAVORITE_ID, 3, "Remove Favorite");
			menu.add(0, RETWEET_ID, 1, "Retweet");
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case REPLY_ID:
			String user = dbActions.userDbQuery(info, DbOpenHelper.TABLE);
			input.setText("@" + user);
			showDialog(0);
			return true;
		case RETWEET_ID:
			if (connHelper.testInternetConnectivity()
					|| prefs.getBoolean("prefDisasterMode", false) == true)
				new Retweet().execute(info.id);
			else
				Toast.makeText(this, "No internet connectivity",
						Toast.LENGTH_LONG).show();
			return true;

		case FAVORITE_ID:
			new SetFavorite(isShowingFavorites).execute(info.id);
			return true;

		case R_FAVORITE_ID:
			new SetFavorite(isShowingFavorites).execute(info.id);
			return true;
		case DELETE_ID:
			if (connHelper.testInternetConnectivity()
					&& prefs.getBoolean("prefDisasterMode", false) == false)
				new DeleteTweet(false).execute(info.id);
			else if (prefs.getBoolean("prefDisasterMode", false) == true) {
				new DeleteTweet(true).execute(info.id);
			} else
				Toast.makeText(this, "No internet connectivity",
						Toast.LENGTH_LONG).show();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (isShowingFavorites)
			changeView(false, DbOpenHelper.TABLE);
		else {
			if (prefs.getBoolean("prefDisasterMode", false) == false)
				super.onBackPressed();
			else
				Toast.makeText(this, "To exit disable disaster mode first",
						Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, REFRESH_ID, 0, "Refresh").setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(0, SEND_ID, 1, "Send tweet").setIcon(
				android.R.drawable.ic_menu_send);
		menu.add(0, MENTIONS_ID, 2, "Mentions").setIcon(
				android.R.drawable.ic_menu_revert);
		menu.add(0, FAVORITES_ID, 3, "Favorites").setIcon(
				android.R.drawable.ic_menu_today);
		menu.add(0, SETTINGS_ID, 4, "Settings").setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(0, DISASTER_ID, 5, "Disaster Table");
		menu.add(0, FRIENDS_ID, 6, "Friends");
		menu.add(0, LOGOUT_ID, 7, "Logout");
		menu.add(0, PUBLISH_ID, 8, "Publish Dis. tweets");
		menu.add(0, EXIT_ID, 9, "Exit");

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (isShowingFavorites) {
			if (menu.findItem(TIMELINE_ID) == null) {
				menu.add(0, TIMELINE_ID, 3, "Timeline").setIcon(
						android.R.drawable.ic_menu_agenda);
				menu.removeItem(FAVORITES_ID);
			}
		} else {
			if (menu.findItem(FAVORITES_ID) == null) {
				menu.add(0, FAVORITES_ID, 3, "Favorites").setIcon(
						android.R.drawable.ic_menu_today);
				menu.removeItem(TIMELINE_ID);
			}
		}
		new Thread(new FetchMentions()).start();
		return true;
	}

	@SuppressWarnings("deprecation")
	private void findMentionDisasterTweets() {
		Cursor cursor = dbActions.disasterDbQuery(null);
		if (cursor != null) {
			if (results == null)
				results = new ArrayList<Status>();
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				for (int i = 0; i < cursor.getCount(); i++) {
					try {
						String text = cursor.getString(cursor
								.getColumnIndexOrThrow(DbOpenHelper.C_TEXT));
						if (text
								.contains("@" + mSettings.getString("user", ""))) {
							String username = cursor
									.getString(cursor
											.getColumnIndexOrThrow(DbOpenHelper.C_USER));
							long id = cursor.getLong(cursor
									.getColumnIndexOrThrow(DbOpenHelper.C_ID));
							long time = cursor
									.getLong(cursor
											.getColumnIndexOrThrow(DbOpenHelper.C_CREATED_AT));
							Date date = new Date(time);
							User user = new User(username);
							Status status = new Status(user, text, id, date);
							results.add(0, status);
						}
						cursor.moveToNext();
					} catch (IllegalArgumentException ex) {
						break;
					}
				}
			}
		}
	}

	private class FetchMentions implements Runnable {
		ContentValues values;

		public void run() {
			if (connHelper.testInternetConnectivity()) {
				if (ConnectionHelper.twitter == null) {
					connHelper.doLogin();
				}
				try {
					if (ConnectionHelper.twitter != null)
						results = (ArrayList<Status>) ConnectionHelper.twitter
								.getMentions();
				} catch (Exception e) {
				}
			}
			findMentionDisasterTweets();
			if (results != null) {
				for (Status status : results) {
					values = DbOpenHelper.statusToContentValues(status);
					try {
						if (db != null)
							if (db.isOpen()) {
								db.insertOrThrow(DbOpenHelper.TABLE_MENTIONS,
										null, values);
							}
					} catch (SQLException ex) {
					}
				}
			}
		}
	}

	// Called when menu item is selected //
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case SETTINGS_ID:
			// Launch Prefs activity
			Intent i = new Intent(this, Prefs.class);
			startActivityForResult(i, PREF_SCREEN);
			return true;
		case EXIT_ID:
			finish();
			return true;
		case DISASTER_ID:
			startActivity(new Intent(this, showDisasterDb.class));
			return true;
		case FRIENDS_ID:
			startActivity(new Intent(this, Friends.class));
			return true;
		case SEND_ID:
			Log.i(TAG, "Sending Tweets- Show Dialog");
			showDialog(0);
			return true;

		case FAVORITES_ID:
			changeView(true, DbOpenHelper.TABLE_FAVORITES);
			return true;
		case REFRESH_ID:
			if (connHelper.testInternetConnectivity())
				new RefreshTimeline(true).execute();
			else
				Toast.makeText(this, "No internet connectivity",
						Toast.LENGTH_LONG).show();
			return true;
		case TIMELINE_ID:
			changeView(false, DbOpenHelper.TABLE);
			return true;
		case MENTIONS_ID:
			startActivity(new Intent(this, Mentions.class));
			return true;
		case LOGOUT_ID:
			if (prefs.getBoolean("prefDisasterMode", false) == false) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.clear();
				editor.commit();
				editor = mSettings.edit();
				editor.clear();
				editor.commit();
				db.execSQL("DROP TABLE IF EXISTS timeline;");
				db.execSQL("DROP TABLE IF EXISTS FavoritesTable;");
				db.execSQL("DROP TABLE IF EXISTS MentionsTable;");
				createTables(db);
				isRunning = false;
				finish();
				startActivity(new Intent(this, MyTwitter.class));
			} else
				Toast.makeText(this, "Disable the disaster mode first",
						Toast.LENGTH_LONG).show();
			return true;
		case PUBLISH_ID:
			publishDisasterTweets(true);
			return true;
		}
		return false;
	}

	private void createTables(SQLiteDatabase db) {
		String sql = getResources().getString(R.string.sql);
		String sqlFavorites = getResources().getString(R.string.sqlFavorites);
		String sqlMentions = getResources().getString(R.string.sqlMentions);
		db.execSQL(sql); // execute the sql
		db.execSQL(sqlFavorites);
		db.execSQL(sqlMentions);
	}

	private void changeView(boolean isShowing, String table) {
		// Get the data from the DB
		cursor = db.query(table, null, null, null, null, null,
				DbOpenHelper.C_CREATED_AT + " DESC");
		if (cursor.getCount() > 0) {
			startManagingCursor(cursor);
			Cursor cursorPictures = db.query(DbOpenHelper.TABLE_PICTURES, null,
					null, null, null, null, null);
			cursorPictures.moveToFirst();
			// Setup the adapter
			adapter = new TimelineAdapter(this, cursor, cursorPictures);
			listTimeline.setAdapter(adapter);
			registerForContextMenu(listTimeline);
			isShowingFavorites = isShowing;
		} else
			Toast.makeText(this, "There are no favorite tweets",
					Toast.LENGTH_LONG).show();
	}

	class RefreshTimeline extends AsyncTask<Void, Void, Boolean> {
		ProgressDialog postDialog;
		boolean showDialog;

		public RefreshTimeline(boolean showDialog) {
			this.showDialog = showDialog;
		}

		@Override
		protected void onPreExecute() {
			if (showDialog)
				postDialog = ProgressDialog.show(Timeline.this,
						"Refreshing Timeline",
						"Please wait while your timeline is being refreshed",
						true, // indeterminate duration
						false); // not cancel-able
		}

		@Override
		protected Boolean doInBackground(Void... message) {
			cancelNotification();
			if (ConnectionHelper.twitter == null) {
				connHelper.doLogin();
			}
			try {
				if (ConnectionHelper.twitter != null) {
					ConnectionHelper.twitter.setCount(40);
					List<Twitter.Status> timeline = ConnectionHelper.twitter
							.getHomeTimeline();
					for (Twitter.Status status : timeline) {

						dbActions.insertIntoTimelineTable(status);
					}
					return true;
				} else
					return false;
			} catch (Exception e) {
				Log.i(TAG, "Updater.run exception: ");
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (showDialog)
				postDialog.dismiss();
			if (result) {
				cursor.requery();
				if (showDialog)
					Toast.makeText(Timeline.this, "Timeline updated",
							Toast.LENGTH_LONG).show();
			} else {
				if (showDialog)
					Toast.makeText(Timeline.this, "Timeline not updated",
							Toast.LENGTH_LONG).show();
			}
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// Can be used to send messages to the service	
		}

		public void onServiceDisconnected(ComponentName className) {
			// Rarely called don't rely on this method				
		}
	};
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PREF_SCREEN:
			if (resultCode == Prefs.BLUE_ENABLED) {
				 bindService(new Intent("tweet.sensor.START_SERVICE"), mConnection, Context.BIND_AUTO_CREATE);
				 mBound = false; 				
				 startServices();
			} else if (resultCode == Prefs.DIS_MODE_DISABLED) {
				
				publishDisasterTweets(true);
				stopService(new Intent(this, DisasterOperations.class));
				try {
					unregisterReceiver(airplaneModeReceiver);
				} catch (Exception ex) {
				}
				SharedPreferences.Editor editor = mSettings.edit();
				editor.remove("firstBroadcastTime"); // battery
				editor.remove("numberOfContacts");
				editor.remove("numberOfConnAttempts");
				editor.remove("isFullSavingActive");
				editor.remove("isPartialSavingActive");
				editor.commit();

			}
		}
	}

	private void startServices() {
		String where = DbOpenHelper.C_MET_AT + "<"
				+ (new Date().getTime() - 2 * 3600 * 1000);
		db.delete(DbOpenHelper.TABLE_ADDRESSES, where, null);
		startService(new Intent(this, DisasterOperations.class));
		startService(new Intent(this, DevicesDiscovery.class));
		startService(new Intent(this, RandomTweetGenerator.class));
		IntentFilter filter = new IntentFilter(ACTION_NEW_DISASTER_TWEET);
		this.registerReceiver(twitterStatusReceiver, filter);
		filter = new IntentFilter("android.intent.action.SERVICE_STATE");
		this.registerReceiver(airplaneModeReceiver, filter);
		this.registerReceiver(sensorDataReceiver, new IntentFilter("android.intent.action.VIEW"));
	}

	private class PostDisasterTweets implements Runnable {

		public void run() {
			try {
				for (int i = 0; i < cursorDisaster.getCount(); i++) {
					long id = cursorDisaster.getLong(cursorDisaster
							.getColumnIndexOrThrow(DbOpenHelper.C_ID));
					String userDb = cursorDisaster.getString(cursorDisaster
							.getColumnIndexOrThrow(DbOpenHelper.C_USER));
					int isFromServ = cursorDisaster
							.getInt(cursorDisaster
									.getColumnIndexOrThrow(DbOpenHelper.C_ISFROMSERVER));
					int hasBeenSent = cursorDisaster.getInt(cursorDisaster
							.getColumnIndexOrThrow(DbOpenHelper.C_HASBEENSENT));
					int isValid = cursorDisaster.getInt(cursorDisaster
							.getColumnIndexOrThrow(DbOpenHelper.C_IS_VALID));
					String user = mSettings.getString("user", "");

					if (user.equals(userDb) && isFromServ == FALSE
							&& hasBeenSent == FALSE && isValid == TRUE) {
						// publish my tweets that have not been sent
						String status = cursorDisaster.getString(cursorDisaster
								.getColumnIndexOrThrow(DbOpenHelper.C_TEXT));
						if (!status.contains("random tweet")) {
							setStatus(id, status);
						}

					} else if (isFromServ == TRUE && hasBeenSent == FALSE) {
						// publish the retweets
						contextActions.retweet(id, DbOpenHelper.TABLE);
					}
					if (!cursorDisaster.isLast())
						cursorDisaster.moveToNext();
					else
						break;

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
				new RefreshTimeline(false).execute();
			} catch (IllegalArgumentException ex) {
			}
		}

	}

	private void publishDisasterTweets(boolean show) {
		if (connHelper.testInternetConnectivity()) {
			cursorDisaster = dbActions
					.disasterDbQuery(DbOpenHelper.C_HASBEENSENT + "=" + FALSE);
			if (cursorDisaster != null) {
				if (cursorDisaster.getCount() != 0) {
					Log.d(TAG, "cursorDisaster.getCount() != 0 = "
							+ cursorDisaster.getCount());
					cursorDisaster.moveToFirst();
					if (show)
						Toast.makeText(this, "Publishing disaster tweets",
								Toast.LENGTH_SHORT).show();
					new Thread(new PostDisasterTweets()).start();
				}
			}
		}
	}

	void sendMessage(String status) {
		boolean isDisaster = prefs.getBoolean("prefDisasterMode", false);
		// send a msg if there is connectivity and length is ok (or if we are in
		// disaster mode)
		if (connHelper.testInternetConnectivity() || isDisaster == true) {
			if (status.length() > 0 && status.length() <= 140) { // if length is
																	// ok
				new SendTask(0, status, isDisaster).execute(); // try to send it
			} else if (status.length() > 140) {
				Toast.makeText(this, "The message is too long ",
						Toast.LENGTH_LONG).show();
			} else
				Toast.makeText(this, "No text to be sent ", Toast.LENGTH_LONG)
						.show();
		} else
			Toast.makeText(this, "No Internet connection", Toast.LENGTH_LONG)
					.show();
	}

	private boolean setStatus(long id, String status) {// I need an id since it
														// is used for publish
														// disaster tweets as
														// well
		if (ConnectionHelper.twitter == null) {
			System.out.println(" Time Line setStatus login");
			connHelper.doLogin();
		}
		try {
			if (ConnectionHelper.twitter != null) {
				ConnectionHelper.twitter.setStatus(status);
				Log.i(TAG, "tweet published");
				if (id != 0) { // in case we are automatically publishing
								// disaster tweets
					dbActions.updateDisasterTable(id, TRUE, TRUE);
					dbActions.delete(DbOpenHelper.C_ID + "=" + id,
							DbOpenHelper.TABLE);
				}
				return true;
			} else
				return false;
		} catch (Exception ex) {
			Log.i(TAG, "tweet not published");
			return false;
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
			postDialog = ProgressDialog.show(Timeline.this, "Sending message",
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
					if (connHelper.testInternetConnectivity())
						new RefreshTimeline(false).execute();
					Toast.makeText(Timeline.this, "Tweet has been sent",
							Toast.LENGTH_SHORT).show();
					hasBeenSent = TRUE;
				}
			} else { // if it hasnt been sent
				if (id == 0) // and it is a normal sending
					Toast.makeText(Timeline.this, "Tweet not sent",
							Toast.LENGTH_SHORT).show();
			}

			if (isDisaster == true) { // if we are in disaster mode
				if (status.length() > 0 && status.length() <= 140) { // and
																		// length
																		// is ok
					String user = mSettings.getString("user", "not found");
					String concatenate = status.concat(" " + user);
					if (dbActions.saveIntoDisasterDb(concatenate.hashCode(),
							new Date().getTime(), new Date().getTime(), status,
							user, "", FALSE, hasBeenSent, TRUE, 0)) {
						String mac = mBtAdapter.getAddress();
						long time = new Date().getTime();
						try {
							if (RandomTweetGenerator.generatorWriter != null)
								RandomTweetGenerator.generatorWriter.write(mac
										+ ":" + user + ":" + status.hashCode()
										+ ":manual:" + time + ":"
										+ new Date().toString() + "\n");
						} catch (IOException e) {
						}
						// if it has been added successfully
						sendBroadcast(new Intent(ACTION_NEW_DISASTER_TWEET));
						if (!result) { // if it has not been successfully I need
										// to show in the timeline
							// otherwise it means i will receive it from online
							// central servers
							dbActions.copyIntoTimelineTable(concatenate
									.hashCode(), new Date().getTime(), status,
									user, FALSE);
							cursor.requery();
						}
					}
					hasBeenSent = FALSE;

				}
				input.setText("");
			}
		}
	}

	class DeleteTweet extends AsyncTask<Long, Void, Boolean> {
		ProgressDialog postDialog;
		boolean isDisaster;

		public DeleteTweet(boolean isDisaster) {
			this.isDisaster = isDisaster;
		}

		@Override
		protected void onPreExecute() {
			postDialog = ProgressDialog.show(Timeline.this,
					"Deleting the message",
					"Please wait while your message is being deleted", true, // indeterminate
																				// duration
					false); // not cancel-able
		}

		@Override
		protected Boolean doInBackground(Long... id) {
			return (contextActions.deleteTweet(id[0], isDisaster));
		}

		// This is in the UI thread, so we can mess with the UI
		@Override
		protected void onPostExecute(Boolean result) {
			postDialog.dismiss();
			cursor.requery();
			if (!isDisaster) {
				if (!result)
					Toast.makeText(Timeline.this, "Unable to delete the tweet",
							Toast.LENGTH_SHORT).show();
			} else
				Toast.makeText(Timeline.this, "Disaster Tweet deleted",
						Toast.LENGTH_SHORT).show();

		}
	}

	class Retweet extends AsyncTask<Long, Void, Boolean> {
		ProgressDialog postDialog;

		@Override
		protected void onPreExecute() {
			postDialog = ProgressDialog.show(Timeline.this, "Posting retweet",
					"Please wait while the retweet is being posted", true, // indeterminate
																			// duration
					false); // not cancel-able
		}

		@Override
		protected Boolean doInBackground(Long... id) {
			return (contextActions.retweet(id[0], DbOpenHelper.TABLE));
		}

		// This is in the UI thread, so we can mess with the UI
		@Override
		protected void onPostExecute(Boolean result) {
			postDialog.dismiss();
			if (result)
				Toast.makeText(Timeline.this, "Retweet posted succesfully",
						Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(Timeline.this, "Retweet not posted",
						Toast.LENGTH_SHORT).show();

		}
	}

	class SetFavorite extends AsyncTask<Long, Void, Boolean> {
		ProgressDialog postDialog;
		boolean isShowingFavorite;

		public SetFavorite(boolean isShowingFav) {
			isShowingFavorite = isShowingFav;
		}

		@Override
		protected void onPreExecute() {
			postDialog = ProgressDialog.show(Timeline.this, "Setting favorite",
					"Please wait while setting as favorite tweet", true, // indeterminate
																			// duration
					false); // not cancel-able
		}

		@Override
		protected Boolean doInBackground(Long... id) {
			return contextActions.favorite(id[0], isShowingFavorites);
		}

		// This is in the UI thread, so we can mess with the UI
		@Override
		protected void onPostExecute(Boolean result) {
			postDialog.dismiss();
			if (!isShowingFavorites) {
				if (result)
					Toast.makeText(Timeline.this, "Favorite set succesfully",
							Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(Timeline.this, "Favorite not set",
							Toast.LENGTH_SHORT).show();
			} else {
				if (result)
					Toast.makeText(Timeline.this,
							"Favorite removed succesfully", Toast.LENGTH_SHORT)
							.show();
				else
					Toast.makeText(Timeline.this, "Favorite not removed",
							Toast.LENGTH_SHORT).show();

				cursor = db.query(DbOpenHelper.TABLE_FAVORITES, null, null,
						null, null, null, null);
				if (cursor.getCount() == 0)
					changeView(false, DbOpenHelper.TABLE);
				else {
					Cursor cursorPictures = db.query(
							DbOpenHelper.TABLE_PICTURES, null, null, null,
							null, null, null);
					adapter = new TimelineAdapter(Timeline.this, cursor,
							cursorPictures);
				}
				listTimeline.setAdapter(adapter);
				registerForContextMenu(listTimeline);
			}
		}
	}

	private final BroadcastReceiver twitterStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// cursor.requery();
			Log.d(TAG, "twitterStatusReceiver inside onReceive ");
			cursor = db.query(DbOpenHelper.TABLE, null, null, null, null, null,
					DbOpenHelper.C_CREATED_AT + " DESC", "100");
			Cursor cursorPictures = db.query(DbOpenHelper.TABLE_PICTURES, null,
					null, null, null, null, null);
			cursorPictures.moveToFirst();
			// Setup the adapter
			adapter = new TimelineAdapter(Timeline.this, cursor, cursorPictures);
			listTimeline.setAdapter(adapter);
		}
	};

	public final BroadcastReceiver sensorDataReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();

			if (extras != null) {
				String userInfo = extras.getString("userInfoStr");
				Log.d("TweetSensor", "TweetSensor UserInfo" + userInfo);
				sendMessage(userInfo);
			}

			Log.d("TweetSensor", "TweetSensor Activity Received");
		}
	};
	
	private final BroadcastReceiver airplaneModeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Service state changed");
			int isModeOn = Settings.System.getInt(context.getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 0);
			if (isModeOn == TRUE) {
				if (prefs.getBoolean("prefDisasterMode", false) == true) {
					stopService(new Intent(Timeline.this,
							DisasterOperations.class));
					Toast
							.makeText(
									Timeline.this,
									"Airplane mode on, disaster mode will be restarted as soon as it will be switch off",
									Toast.LENGTH_LONG).show();
					mBtAdapter.disable();
				}
			} else if (isModeOn == FALSE) {
				if (prefs.getBoolean("prefDisasterMode", false) == true) {
					startService(new Intent(Timeline.this,
							DisasterOperations.class));
					startService(new Intent(Timeline.this,
							DevicesDiscovery.class));
					startService(new Intent(Timeline.this,
							RandomTweetGenerator.class));
					// Toast.makeText(Timeline.this,
					// "Disaster Mode has been restarted",
					// Toast.LENGTH_LONG).show();
				}

			}
		}
	};
	
	
	private final BroadcastReceiver externalAppReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle data = intent.getExtras();
			String userInfo = data.getString("UserInfoStr");
			Log.d(TAG, "sensorDataReceived ");
			Toast.makeText(context, "sensorDataReceived: " + userInfo,
					Toast.LENGTH_SHORT).show();
		}
	};


}