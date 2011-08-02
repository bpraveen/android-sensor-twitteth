package com.example;

import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.Twitter.Status;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


/**
 * Responsible for pulling twitter updates from twitter.com and putting it into
 * the database.
 */
public class UpdaterService extends Service {
  static final String TAG = "UpdaterService";
  Handler handler;
  Updater updater; 
  //SQLiteDatabase db = MyTwitter.db;
  static  SQLiteDatabase db;
  TweetDbActions dbActions;
  Thread updaterThread;
  DbOpenHelper dbHelper;  
  ConnectivityManager connec;
  SharedPreferences mSettings;
  ConnectionHelper connHelper;
  
  static final String ACTION_NEW_TWITTER_STATUS = "ACTION_NEW_TWITTER_STATUS";
  Twitter twitter = null;

  @Override
  public void onCreate() {
    super.onCreate();
    // Setup handler
    handler = new Handler();  
    dbHelper = new DbOpenHelper(this);
  	db = dbHelper.getWritableDatabase(); 
  	mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);   
	connec =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
	connHelper = new ConnectionHelper(mSettings,connec);
	if (connHelper.testInternetConnectivity()) {
		connHelper.doLogin();
		twitter = ConnectionHelper.twitter;
	}
	
  }

  @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    updater = new Updater();
	    dbActions = new TweetDbActions();
	    updaterThread = new Thread(updater);
	    updaterThread.start();       
	    Log.d(TAG, "onStart'ed");
	    return START_STICKY;
	}
  @Override
  public void onDestroy() {
    super.onDestroy();
    handler.removeCallbacks(updater); // stop the updater  
    Log.d(TAG, "onDestroy'd");
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  /** Updates the database from twitter.com data */
  class Updater implements Runnable {
	  static final int NOTIFICATION_ID = 47;
	    static final long DELAY = 300000L;
	    Notification notification;
	    NotificationManager notificationManager;
	    PendingIntent pendingIntent;
	    
	    Updater() {
	    	notificationManager = (NotificationManager) UpdaterService.this
	    	.getSystemService(Context.NOTIFICATION_SERVICE);
	    	notification = new Notification( android.R.drawable.stat_sys_download,
	    			"Twitteth", System.currentTimeMillis() );
	    	 pendingIntent = PendingIntent.getActivity(UpdaterService.this, 0,
	    	          new Intent(UpdaterService.this, MyTwitter.class), 0);	    	
	    }

    public void run() {
      Log.d(UpdaterService.TAG, "Updater ran.");
      boolean haveNewStatus =  false;

      try { 
    	if (connHelper.testInternetConnectivity()) {
    		if (twitter !=null) {    		
    			twitter.setCount(40);
    			List<Status> timeline = twitter.getHomeTimeline();
    			for (Status status : timeline) {
            		 if( dbActions.insertIntoTimelineTable(status))
            			 haveNewStatus = true;
    			}
    		}
    		else {
    			connHelper.doLogin();
    			twitter = ConnectionHelper.twitter;
    		}
    	}    	
      } catch (Exception e) {
        Log.e(TAG, "Updater.run exception: " + e);               
      } 
      if (haveNewStatus) {
    	  sendBroadcast(new Intent(ACTION_NEW_TWITTER_STATUS));
    	  Log.d(TAG, "run() sent ACTION_NEW_TWITTER_STATUS broadcast.");

          // Create the notification
    	  notification.setLatestEventInfo(UpdaterService.this, "New Twitter Status",
    			  "You have new tweets in the timeline", pendingIntent );
    	  notification.when = System.currentTimeMillis();    	       	 
          notificationManager.notify(NOTIFICATION_ID, notification);     	  
      }      
      // Set this to run again later
      handler.postDelayed(this, DELAY);
    }
  }


}