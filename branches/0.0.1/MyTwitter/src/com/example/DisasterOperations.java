package com.example;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class DisasterOperations extends Service {
	
	//Message types sent from the BluetoothService Handler	  
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_DELAY = 6;
	public static final int MESSAGE_TOAST =8 ;
	  
	// Key names received from the BluetoothService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String DEVICE_ADDRESS = "device address";
	public static final String TOAST = "toast";
	public static final String DEVICE = "device";
	public static final String PEERS_MET = "peers_met";
	
	static final int FALSE = 0;
	static final int TRUE = 1; 	
	
	private static final String TAG = "DisasterOperations";
	// Member object for the chat services
	static BluetoothComms mBlueService = null;
	ConnectToDevices connectToDevices;
	private Cursor cursorDisaster,cursorPeers;
	SharedPreferences mSettings,prefs;  
	private int numberOfConnAttempts,numberOfContacts;	
	static int numberOfTweets;
	ConnectionHelper connHelper;
	boolean isPartialSavingActive,isFullSavingActive,notify;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	private String mConnectedDeviceAddress =  null;
	private ArrayList<BluetoothDevice> devicesArrayList = null; 
	TweetDbActions dbActions;
	TweetContextActions contextActions;  
	WakeLock wakeLock;
	Handler handler;	
	boolean peerRequestedClosing =false;	 
	CheckState checkState = null;
	long startingTime,now, firstBroadcastTime = 0;
	String startingDate;
	private BluetoothAdapter mBtAdapter;
	
	FileWriter batteryWriter,rxTweetsWriter,startTimeWriter,contactsWriter,
		customExceptionWriter,contactsTimestampsWriter;
	FileWriter neighboursWriter,connAttemptsWriter;
	LogFilesOperations logOps;
	ConnectionAttemptTimeout connTimeout;
	String user;
	ArrayList<BluetoothDevice> pairedDevices;
	DbOpenHelper dbHelper;
	static int connAttemptsSucceded=0;
	
 @Override
	public void onCreate() {
		
	 super.onCreate();			
		prefs = PreferenceManager.getDefaultSharedPreferences(this);	    
		mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);		 
		user = mSettings.getString("user", "");
		
		isPartialSavingActive =mSettings.getBoolean("isPartialSavingActive", false);
		isFullSavingActive = mSettings.getBoolean("isFullSavingActive", false);
		ConnectivityManager connec =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		connHelper = new ConnectionHelper(mSettings,connec);
	    
	    dbActions = new TweetDbActions();
	    contextActions = new TweetContextActions(connHelper,prefs);
		           
        // Register for broadcasts when a device is discovered
	    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	    this.registerReceiver(mReceiver, filter);
	    // Register for broadcasts when discovery has finished
	    filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	    this.registerReceiver(mReceiver, filter);  
	    	    
	    registerReceiver(mBatInfoReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    mBtAdapter = BluetoothAdapter.getDefaultAdapter();   
	    devicesArrayList = new  ArrayList<BluetoothDevice>();
	    handler = new Handler();
	    connectToDevices = new ConnectToDevices();		
	  
	    PowerManager mgr = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
	    wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
	    wakeLock.acquire();	
	    
	    createLogFiles();
	 // Initialize the BluetoothChatService to perform bluetooth connections
	    mBlueService = new BluetoothComms(this, mHandler);    	      
        mBlueService.start(); 
        /*
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 10);
        AlarmManager alManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("timeout");	 	
	 	 // In reality, you would want to have a static variable for the request code instead of 192837
	 	PendingIntent sender = PendingIntent.getBroadcast(this, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);

	 	alManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 12*3600*1000,sender );
	 	*/
	 //	registerReceiver(timeoutReceiver,new IntentFilter("timeout"));
	 	  
        saveNeighbourMac(mBtAdapter.getAddress()); //save my mac address  
      //  loadNeighboursMac();
	   Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());
        
       /* pairedDevices = new ArrayList<BluetoothDevice>();
   	 	BluetoothDevice device1 = mBtAdapter.getRemoteDevice("F8:DB:7F:0E:AC:8A"); 
   	 	if (!mBtAdapter.getAddress().equals(device1.getAddress()))
   	 		pairedDevices.add(device1);
   	 	BluetoothDevice device2 = mBtAdapter.getRemoteDevice("F8:DB:7F:0E:AA:43");
   	 	if (!mBtAdapter.getAddress().equals(device2.getAddress()))
   	 		pairedDevices.add(device2); */
 }
 
 
private void loadNeighboursMac() {		
}


private void createLogFiles() {		
	 //creating files for logs
    logOps = new LogFilesOperations();
    logOps.createLogsFolder();
    batteryWriter = logOps.createLogFile("Battery_" + user );
    rxTweetsWriter = logOps.createLogFile("Received_Tweets_" + user); // no saving into shared preferences
    startingTime = new Date().getTime();
    startingDate = new Date().toString();
    startTimeWriter = logOps.createLogFile("startingTime_" + user);	 // no saving into shared preferences
    contactsWriter = logOps.createLogFile("numberOfContacts_" + user);
    neighboursWriter = logOps.createLogFile("NeighboursMac_" + user);
    connAttemptsWriter = logOps.createLogFile("connAttempts_" + user);
    contactsTimestampsWriter = logOps.createLogFile("contactsTimestamps_" + user);
}

private void getDatabase() {
	// Initialize DB
  	 // dbHelper = new DbOpenHelper(this);  	  
  	//  MyTwitter.db = dbHelper.getWritableDatabase();    		
  }

 public class CustomExceptionHandler implements UncaughtExceptionHandler {
 
	public void uncaughtException(Thread t, Throwable e) {		    		
		Log.e(TAG, "CustomExceptionHandler", e);
		customExceptionWriter = logOps.createLogFile("CustomException_" + user);
		try {
			customExceptionWriter.write("CustomExceptionHandler executed " + new Date().toString() + "\n");
			Process process = Runtime.getRuntime().exec("logcat -d -t 500 *:E");
		    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		    StringBuilder log=new StringBuilder();
		    String line;
		    while ((line = bufferedReader.readLine()) != null) {
		        log.append(line + "\n");
		    }		    
			customExceptionWriter.write(log.toString() + "\n");
		} 
		catch (IOException e1) {	}	
		
		notifyCrash();		
		closeService();
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putInt("connAttemptsSucceded",connAttemptsSucceded );
		editor.commit();
		DisasterOperations.this.stopSelf();
		AlarmManager mgr = (AlarmManager) MyTwitter.activity.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, MyTwitter.restartIntent);
		System.exit(2);
	}
}

@Override
 public IBinder onBind(Intent intent) {
	return null;
  }

private void saveNeighbourMac(String mac) {
	try {
		neighboursWriter.write(mac + "\n");
	} catch (IOException e) {}
 }	

private void closeLogFiles() {
	try {
		startTimeWriter.write("starting time:" + startingTime + ":" + startingDate  + "\n");		
		startTimeWriter.close();
		batteryWriter.close();
		rxTweetsWriter.close();
		contactsWriter.write("number of contacts till " + new Date().toString() + ":" + numberOfContacts + "\n");
		contactsWriter.close();
		connAttemptsWriter.write("number of successful attempts till " + new Date().toString() + ":" + connAttemptsSucceded + "\n");
		connAttemptsWriter.write("number of attempts till " + new Date().toString() + ":" + numberOfConnAttempts + "\n");
		connAttemptsWriter.close();
		contactsTimestampsWriter.close();
		neighboursWriter.close();
		if (customExceptionWriter != null)
			customExceptionWriter.close();
	  } 
	catch (IOException e) {}
}

private void closeService() {	
    handler.post(new ShutDownDelayed());   
    closeLogFiles();  
	 
	 // Unregister broadcast listener   
		 try {
			 unregisterReceiver(mReceiver);  
			 unregisterReceiver(mBatInfoReceiver);
		 } catch (Exception ex) {
			 Log.e(TAG,"error unregisterReceiver ",ex);
		 }		 
		 if (!isFullSavingActive) {
			 stopService(new Intent(this,DevicesDiscovery.class));
			 stopService(new Intent(this,RandomTweetGenerator.class));
		 }
		 handler.removeCallbacks(connectToDevices);  
		 wakeLock.release();	 
}

@Override
public void onDestroy() { 
	  Log.i(TAG,"inside on destroy, stopping everything");	
	closeService();	 
	connAttemptsSucceded=0;
	super.onDestroy();	       
}
		 
 private class ShutDownDelayed implements Runnable {
		
		public void run() {
			if (mBlueService != null) {
			    	  if (mBlueService.getState() != BluetoothComms.STATE_CONNECTING &&
			    			  mBlueService.getState() != BluetoothComms.STATE_CONNECTED) {
			    		mBlueService.stop();
			    		mBlueService = null;   	  		    			 
			    	  } 
			    	  else
			    		handler.postDelayed(this, 2000);  
			    	}
		}
	}
 
 // The BroadcastReceiver that listens for discovered devices 
 private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
     @Override
     public void onReceive(Context context, Intent intent) {
   	  try {
         String action = intent.getAction();

         // When discovery finds a device
         if (BluetoothDevice.ACTION_FOUND.equals(action)) {
       	  
             // Get the BluetoothDevice object from the Intent
             BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);            	  
          // If it's already paired, skip it
             if (device.getBondState() != BluetoothDevice.BOND_BONDED) {            	  
            	 if (!devicesArrayList.contains(device)){    
            		 if (device.getBluetoothClass().getDeviceClass() == 
            			 BluetoothClass.Device.PHONE_SMART) {
        				// saveNeighbourMac(device.getAddress());
            			 devicesArrayList.add(device); 
            			 Log.i(TAG,device.getName() + " added"); 
           		     }
              	}           	
             }               
         // When discovery is finished...
         } else  if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {         
       	  
       	  // Get a set of currently paired devices       	 
             Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();        	 
             // If there are paired devices, add each one to the ArrayAdapter
             if (pairedDevices.size() > 0) {                
                 for (BluetoothDevice device : pairedDevices) {
                     devicesArrayList.add(device);
                     Log.i(TAG,"added paired device: " + device.getAddress());
                 }
             } 
       	    Log.i(TAG,"Discovery finished" );        	 
       	    if (mBlueService.getState() != BluetoothComms.STATE_CONNECTED) {        	    	         	    	
       	    	long delay = Math.round(Math.random()*2000);        	    	
       	    	handler.postDelayed(connectToDevices,delay);      	    	   	
       	  		
       	    } else { 
       	    	Log.i(TAG,"state already connected" );      	    	
       	    	checkState = new CheckState(5);
       	    	checkState.execute();
       	    }           	            
         } 
     }
   	  catch (Exception ex) {}
     }
 };
 
 private class CheckState extends AsyncTask<Void, Void, Void> {	
	 int delay;
	 
	 CheckState(int delay) {
		 this.delay = delay;
	 }
	 
		@Override
		protected Void doInBackground(Void... id ) {
			int i=0;
			  try {
			  while (mBlueService.getState() == BluetoothComms.STATE_CONNECTED) {
		    		try {
		    			if (i == delay) {
		    				mBlueService.start();
		    				numberOfTweets = 0;
		    				Log.i(TAG,"CheckState -->reset");
		    			}
		    			Thread.sleep(1000);
		    			i++;		    			
		    		} catch (Exception ex) {}			    		
		    	}						  
			  }
			  catch (Exception ex) {}	
			  return null;
		}		

		// This is in the UI thread, so we can mess with the UI
		@Override
		protected void onPostExecute(Void result) {
			Log.i(TAG,"not connected" );
			handler.post(connectToDevices);				
			checkState =  null;
		}
	}

 private void notifyCrash() {	 
	    Notification notification;
	    NotificationManager notificationManager;
	    PendingIntent pendingIntent;	    
	    
	    notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
	    notification = new Notification( android.R.drawable.stat_sys_download,
	    			"DisasterMode", System.currentTimeMillis() );
	    pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, Timeline.class), 0);	
	    
	 // Create the notification
	  notification.setLatestEventInfo(this, "Crash",
			  "The disaster mode has crashed", pendingIntent );
	  notification.when = System.currentTimeMillis();
	  notification.defaults |= Notification.DEFAULT_VIBRATE;  	 
      notificationManager.notify(20, notification);  	    	  
}
 
 private class ConnectionAttemptTimeout implements Runnable {
	 public void run() {
		 if (mBlueService != null) {		 
			 if (mBlueService.getState() == BluetoothComms.STATE_CONNECTING) {
				 Log.i(TAG,"stopping connection attempt");
				 mBlueService.cancelConnectionAttempt();
			 }
			 connTimeout = null;
	 }
	}
 }
   
 // The Handler that gets information back from the BluetoothChatService
 private final Handler mHandler = new Handler() {
	 
     @Override
     public void handleMessage(Message msg) {
         switch (msg.what) {          
         
         case MESSAGE_READ:         	
             byte[] readBuf = (byte[]) msg.obj;
             // construct a string from the valid bytes in the buffer           
             String readMessage = new String(readBuf);
             Log.i(TAG, "received string: " + readMessage);             
			 splitTweets(readMessage);			               
             break;
             
         case MESSAGE_DEVICE_NAME:
             // save the connected device's name
             mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
             mConnectedDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);           
             Log.i(TAG,"Connected to " + mConnectedDeviceName);
             numberOfTweets = 0;
             if (connTimeout != null) { // I need to remove the timeout started at the beginning
            	 handler.removeCallbacks(connTimeout);
            	 connTimeout = null;
             }
             if (numberOfContacts == 0) { //restore variable in case of crash
				  if (mSettings.contains("numberOfContacts")) {
					  numberOfContacts = mSettings.getInt("numberOfContacts", 0);	
					  Log.i(TAG,"restoring numberOfContacts.. = " + numberOfContacts);
				  }
			 }           
            //query the peers db to see if it has already been met  
             cursorPeers = dbActions.peersDbQuery(mConnectedDeviceAddress);            
             if (cursorPeers != null) {
               int isPresent = cursorPeers.getCount();	               
           	   if (isPresent == 1) {
           		   cursorPeers.moveToFirst();//they have already met             		   
           		   if (!didTheyMeetRecently(cursorPeers) ) {
           			   incrementNumContacts();
           			   sendNumberOfTweets(mConnectedDeviceAddress,false);
           		   }
           		  // else {
           				//Log.i(TAG,"peer met too recently, closing connection");
           				//mBlueService.start();
           		 	 // }
           	   } 
           	   else { //they have not met before
           		   incrementNumContacts();
           		   sendTimestampOfLastTweet();  
           	   }           	   
             }              
             break;              
         case MESSAGE_TOAST:             
             //need to connect to following phone
        	 Log.i(TAG,"Unable to connect to the device" ); 
        	 if (connTimeout != null) {
            	 handler.removeCallbacks(connTimeout);
            	 connTimeout = null;
             }
             handler.post(connectToDevices);             
             break;
         }
     }
 };
 
 private class SentMessageTimeout implements Runnable {
	 int method;
	 
	 public SentMessageTimeout(int method) {
		 this.method = method;
	 }
	 
	 public void run() {
		 if (mBlueService != null) {
			 if (mBlueService.getState() == BluetoothComms.STATE_CONNECTED) {
				 if (method ==1) 				
					sendNumberOfTweets(mConnectedDeviceAddress,true); 				 
				 else 					 
					sendTimestampOfLastTweet();   			 
			 }
		 }
	 }
 }
 
 private void incrementNumContacts() {
	 try {
		String date = new Date().toString(); 
		String date_r = date.replace(":", "-");
		contactsTimestampsWriter.write("connected to:" + mConnectedDeviceAddress + ":" + mConnectedDeviceName + 
				 ":at:" + date_r + ":" + new Date().getTime() + "\n");
	} catch (IOException e) {
		
	}
	 numberOfContacts++;
	 SharedPreferences.Editor editor = mSettings.edit();   
	 editor.putInt("numberOfContacts", numberOfContacts);
	 editor.commit();
	 
 }
 
 private void sendTimestampOfLastTweet() {
	  
	  long recentTweetTime = dbActions.getTimestampLastTweet(mConnectedDeviceName);	  		  
	  if (mBlueService.getState() == BluetoothComms.STATE_CONNECTED)  {
	 	String message = "Most recent tweet received at:" + recentTweetTime;
	    mBlueService.write(message.getBytes());  
	 	Log.i(TAG, "sent: " + message); 
	 	if (checkState == null) {
			  checkState = new CheckState(6);
		  	  checkState.execute();	
		  }
	  } else {Log.i(TAG, "STATE is NOT CONNECTED"); }	  	 
 }
 
 private void sendNumberOfTweets(String address, boolean timeout) {
	  try {	 
		  if (!timeout) {
			  int index = cursorPeers.getColumnIndex(DbOpenHelper.C_TWEETS_NUMBER);
			  if (index != -1)
				  numberOfTweets = cursorPeers.getInt(index);
		  }
	  
		  if (mBlueService.getState() == BluetoothComms.STATE_CONNECTED)  {
			  String message = "The number of tweets seen last time is:" + numberOfTweets;
			  if (mBlueService != null)
				  mBlueService.write(message.getBytes());  
			  Log.i(TAG, "sent: " + message); 
			  if (checkState == null) {
				  checkState = new CheckState(6);
				  checkState.execute();	
			  }
		  } 
	 	 else {Log.i(TAG, "sendNumberOfTweets: STATE is NOT CONNECTED"); }
	  } 
	  catch (IllegalArgumentException ex) {}	  
 }  
 
 private void notifyUser() {	 
	    Notification notification;
	    NotificationManager notificationManager;
	    PendingIntent pendingIntent;	    
	    
	    notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
	    notification = new Notification( android.R.drawable.stat_sys_download,
	    			"DisasterMode", System.currentTimeMillis() );
	    pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, showDisasterDb.class), 0);	
	    
	 // Create the notification
 	  notification.setLatestEventInfo(this, "New Disaster Tweets",
 			  "You have new tweets in the database", pendingIntent );
 	  notification.when = System.currentTimeMillis();
 	 // notification.defaults |= Notification.DEFAULT_VIBRATE;  	
 	 notification.defaults |= Notification.DEFAULT_LIGHTS;
      notificationManager.notify(Timeline.NOTIFICATION_ID, notification);  	    	  
 }
 
 
 private void splitStringFields(String readMessage) {
	  int hasBeenSent = FALSE;
	  	if (readMessage != null) {
	  	  try {
	  		String[] fields = readMessage.split("delimiter");
	  		if (fields.length == 7) {	  			
	  				long id = Long.parseLong(fields[0]);	
	  				long created = Long.parseLong(fields[1]);	  
	  				String status = fields[2];
	  				String user = fields[3];	  				
	    		 	 int isFromServer = Integer.parseInt(fields[4]); //I need to know if it is a retweet spreaded locally   	    		 
	    		 	 if (isFromServer == FALSE)
	    		 		 hasBeenSent = TRUE; //if it is a local tweet I will not publish it so i can set like it has been sent
	    		 	 else
	    		 		 hasBeenSent = Integer.parseInt(fields[5]);	    		 	
	    		 	 int hopCount = Integer.parseInt(fields[6]) + 1;	    		 
	    		 	 // try to save into the disaster table	
	    		 	 if (dbActions != null) {
	    		 		 if (dbActions.saveIntoDisasterDb(id, created, now, status, user,
	    		 				 mConnectedDeviceName,isFromServer,hasBeenSent,TRUE, hopCount)) {	    		 			
	    		 			 // if success notify the user and copy into the timeline table
	    		 			 notify = true;
	    			 		 sendBroadcast(new Intent(Timeline.ACTION_NEW_DISASTER_TWEET));	    			 		
	    			 		 long delay = Math.round( (now/ 60000) - (created / 60000) );	    			 		
	    			 		 try {
	    			 			 if (rxTweetsWriter != null) {
	    			 				 String date =  new Date().toString();
	    			 				 long nowForReceiver = new Date().getTime();
	    			 				rxTweetsWriter.write(id + ":" + user + ":" + now + ":" + delay + ":" 
   			 							 + hopCount + ":" + nowForReceiver + ":" + date + ":" + created + "\n");
	    			 			 }	    			 					 
	    			 		 } catch (IOException e) {	Log.i(TAG,"io exception");	} 
	    			 		 if (!status.contains("random tweet")) {	    			 			 
	    			 			 dbActions.copyIntoTimelineTable(id,created,status,user,isFromServer);	    			 					    	    			 			 
	    			 		 }
	    		 		 }
	  				 }	  				
	  		}  
	  		else Log.i(TAG,"fields number is " + fields.length);
	  	  } 
		  catch (Exception ex) {
			  Log.e(TAG,"error inside splitStringFields: ",ex);
		  }  
       }
             
	 } 
 
 
 private void splitTweets(String tweets) {	 
	  int number = 0;
	  int index = 0;
	  if (tweets.contains("Most recent tweet received at:") && !tweets.contains("TWEETSUPDATE") 
			  && !tweets.contains("~~") ) { 
		  try {
			  Log.d(TAG, "tweets.contains(Most recent tweet received at:) ");
			  index = tweets.indexOf(":");
			  if (tweets.substring(index + 1).length() < 15) {
				  long time = Long.parseLong(tweets.substring(index + 1));			 
				  cursorDisaster = dbActions.disasterDbQuery(null);
			 	  sendRelevantTweets(0,false,time);			 	 
			  }
			  //need to memorize they met so that I can avoid further connections for a while
			  dbActions.savePairedPeer(mConnectedDeviceAddress,numberOfTweets);  
			 } 
		  catch (IndexOutOfBoundsException ex) {}			
		  catch (NumberFormatException ex) {}
	  }		   
	  else if (tweets.contains("The number of tweets seen last time is:")
			   && !tweets.contains("~~")) {		   
		  try {
			  Log.d(TAG, "tweets.contains(The number of tweets seen last time is:) ");
			  try {
			  index = tweets.indexOf(":");
			  } catch(NullPointerException ex) {}
			  if (index != 0) {
				  number = Integer.parseInt(tweets.substring(index + 1));
				  cursorDisaster = dbActions.disasterDbQuery(null);
				  if (cursorDisaster != null) {
		 			 Log.d(TAG, "cursorDisaster != null");  
		 	  		if (  cursorDisaster.getCount() > number  ) {
		 	  			Log.i(TAG,"there are new tweets, sending...");
		 	  			sendRelevantTweets(number,true,0);
		 	  		} 
		 	  		else if (cursorDisaster.getCount() < number) {
		 	  			Log.i(TAG,"I must have crashed so send everything again");
		 	  			sendRelevantTweets(0 ,false,0);		 	  		
		 	  		}
		 	  		else {
		 	  			Log.i(TAG,"there are no new tweets");
		 	  		
		 	  		}					  
				  }	
				  else 
					  getDatabase();
				 dbActions.savePairedPeer(mConnectedDeviceAddress,numberOfTweets);  
		 	 }
		   } 
		  catch (NumberFormatException ex) {
			  Log.d(TAG,"inside exception number tweets");
		  }		  
	  } else if (!tweets.contains("Close connection") && !tweets.contains("The number of tweets seen last time is:")
			  && !tweets.contains("Most recent tweet received at") ) {
		  try {
			  Log.d(TAG, "tweets received");  
			  String[] tweetsArray = tweets.split("~~");
			  if (tweetsArray[0].equals("TWEETSUPDATE") ) {
				  Log.d(TAG, "tweetsArray[0].equals(TWEETSUPDATE)"); 
				  numberOfTweets = Integer.parseInt(tweetsArray[1]);
				  now = Long.parseLong(tweetsArray[2]);				 
				  for (int i=3; i< tweetsArray.length; i++) {		    
					  splitStringFields(tweetsArray[i]);		   
				  }	
				  if (notify) {
					  notifyUser();
					  notify = false;
				  }
			  }
			  else {
				  Log.d(TAG, "first time receiving tweets"); 
				  numberOfTweets = Integer.parseInt(tweetsArray[0]);
				  now = Long.parseLong(tweetsArray[1]);
				  for (int i=2; i< tweetsArray.length; i++) 		    
					  splitStringFields(tweetsArray[i]);
				  if (notify) {
					  notifyUser();
					  notify = false;
				  }
			  }				
			  dbActions.savePairedPeer(mConnectedDeviceAddress,numberOfTweets);			  
		  } catch (NumberFormatException ex) {}		 
	  } 
	  else if (tweets.contains("Close connection"))  {
		  Log.d(TAG, "tweets.contains(Close connection)"); 
		  peerRequestedClosing = true;
		  numberOfTweets = 0;		  
	  }		  	 
	 //-------------------------------------------------------------//		  
	  if (peerRequestedClosing) {
		  if (mBlueService != null) {
   		  mBlueService.start();   	   		
   		  peerRequestedClosing = false;    		  		
   		  handler.post(connectToDevices);
   	  }
	  } else {		  
			  if (cursorDisaster != null ) {			 
				 	 if (cursorDisaster.getCount() != 0 ) { 			
				  			handler.postDelayed(new SendClosingRequest(), 500);	
				  			Log.i(TAG,"sending closing request");
				 	 }	
				 	 else
				 		 getDatabase();
			  }		  		 
			  //timeout
			  if (checkState == null) {
				  checkState = new CheckState(5);
				  checkState.execute();	
			  }		  
		}	
 }  
 
 private boolean didTheyMeetRecently(Cursor cursorPeers) {	 
	try {
		long met_at = cursorPeers.getLong(cursorPeers.getColumnIndexOrThrow(DbOpenHelper.C_MET_AT));
		 String mac =  cursorPeers.getString(cursorPeers.getColumnIndexOrThrow(DbOpenHelper.C_MAC));
		   long diff = (new Date().getTime() - met_at); 				
		  // Log.i(TAG,mac +" met " + TimeUnit.MILLISECONDS.toSeconds(diff) + " seconds ago");
		   if (TimeUnit.MILLISECONDS.toSeconds(diff) < 100 )
			   return true;
		   else
			   return false;
	} catch (IllegalArgumentException ex) {
		return false;
	}
 }
 
 private class ConnectToDevices implements Runnable {	
	  
	  public void run( ) {
		  
		  if (!devicesArrayList.isEmpty()) {		   
			//remove the devices that have been met recently
			for (int i=0; i< devicesArrayList.size(); i++) {
				 cursorPeers = dbActions.peersDbQuery(devicesArrayList.get(i).getAddress());
				 if (cursorPeers != null) {
	            	   if (cursorPeers.getCount() == 1) {
	            		   cursorPeers.moveToFirst();	            		   
	            		   if (didTheyMeetRecently(cursorPeers) ) {
	            			   Log.i(TAG,"removing device since it has been met too recently");
	            			   try {
	            				   devicesArrayList.remove(i);  	            				   
	            			   } 
	            			   catch (IndexOutOfBoundsException ex) {}
	            		   }
	            	   }
	            	  cursorPeers.moveToNext();
	               }  	 				
			}
		  }
		  if (!devicesArrayList.isEmpty()) {
			 if (mBlueService != null ) { 				 
				  if (mBlueService.getState() != BluetoothComms.STATE_CONNECTED &&
						  mBlueService.getState() != BluetoothComms.STATE_CONNECTING) { 					
					  try {
						 BluetoothDevice phone = devicesArrayList.remove(0);
						// Attempt to connect to the device
	 					 Log.i(TAG,"connection attempt to " + phone.getName());	 					  	 					  
	 					 connTimeout = new ConnectionAttemptTimeout();
	 					 handler.postDelayed(connTimeout, 10000); //timeout for the conn attempt	 	
	 					 mBlueService.connect(phone);	
	 					 //update for the logs
	 					if (connAttemptsSucceded == 0) {
	 						 //i need to check whether we are restarting after crashing or not
	 						  if (mSettings.contains("connAttemptsSucceded")) {
	 							 connAttemptsSucceded = mSettings.getInt("connAttemptsSucceded", 0);
	 							 Log.i(TAG,"restoring connAttemptsSucceded.. = " + connAttemptsSucceded);	 							 
	 						  }
	 				  }
		 				  if (numberOfConnAttempts == 0) {
		 						 //i need to check whether we are restarting after crashing or not
		 						  if (mSettings.contains("numberOfConnAttempts")) {
		 							 numberOfConnAttempts = mSettings.getInt("numberOfConnAttempts", 0);
		 							 Log.i(TAG,"restoring numberOfConnAttempts.. = " + numberOfConnAttempts);
		 							 
		 						  }
		 				  }	
		 				  numberOfConnAttempts++;
		 		          SharedPreferences.Editor editor = mSettings.edit();   
		 		          editor.putInt("numberOfConnAttempts", numberOfConnAttempts);
		 		   	      editor.commit();
						 } catch (IndexOutOfBoundsException ex) {}
				  } else
					  handler.postDelayed(connectToDevices, 2000); 					  
			  }	  
			}	   		 	
	  }
 } 
 private class SendClosingRequest implements Runnable {	
	  
	  public void run( ) {
		  String messageClose = "Close connection";
		  if (mBlueService != null) {
			  mBlueService.write(messageClose.getBytes());
			  Log.i(TAG,"closing request has been sent");
		  }
		  cursorDisaster = null;			 		 		
	  }
}     
 
 
 private void sendRelevantTweets(long number, boolean isUpdate, long time) {	 
	    if (cursorDisaster != null) {
	    boolean enableSent_by = false;
	    String messages = cursorDisaster.getCount() + "~~" + new Date().getTime() + "~~";
	    if (cursorDisaster.getCount() != 0) {	    
	    	cursorDisaster.moveToFirst();
	    	
	     if (isUpdate) 	    		
	      messages = "TWEETSUPDATE~~" + cursorDisaster.getCount() + "~~" + new Date().getTime() + "~~" ;
	    	// send all the new messages. I subtract from the total number the old ones.
	     for (int i =0; i< (cursorDisaster.getCount()-number); i++) {	    		
	    	String user = cursorDisaster.getString(cursorDisaster.getColumnIndex(DbOpenHelper.C_USER));
	    	String sent_by = cursorDisaster.getString(cursorDisaster.getColumnIndex(DbOpenHelper.C_SENT_BY));
	    	String status = cursorDisaster.getString(cursorDisaster.getColumnIndex(DbOpenHelper.C_TEXT));
	    		// i dont wanna send tweets to the author who gave them to me, so i must check it
	    	if (time == 0)
	    		enableSent_by = true;
	    	if (!user.equals(mConnectedDeviceName) && ( ( !sent_by.equals(mConnectedDeviceName) ) || enableSent_by  ) ) {
	    		int isValid = cursorDisaster.getInt(cursorDisaster.getColumnIndex(DbOpenHelper.C_IS_VALID));
		    	if (isValid == TRUE) {
		    		long received = cursorDisaster.getLong(cursorDisaster.getColumnIndex(DbOpenHelper.C_ADDED_AT)) ;
		    		//if it is the first encounter i send everything older than the timestamp received
		    		//that represents the most recent information received somehow from other nodes
		    		if (received > time ) {
		    			long created = cursorDisaster.getLong(cursorDisaster.getColumnIndex(DbOpenHelper.C_CREATED_AT)) ;
		    			long id = cursorDisaster.getLong(cursorDisaster.getColumnIndex(DbOpenHelper.C_ID));		    			
		    			int isFromServ = cursorDisaster.getInt(cursorDisaster.getColumnIndex(DbOpenHelper.C_ISFROMSERVER));
		    			int hasBeenSent = cursorDisaster.getInt(cursorDisaster.getColumnIndex(DbOpenHelper.C_HASBEENSENT));
		    			int hopCount = cursorDisaster.getInt(cursorDisaster.getColumnIndex(DbOpenHelper.C_HOPCOUNT));
		    			String tweet = "" + id + "delimiter" + created + "delimiter" + status + "delimiter" +
		    			user + "delimiter" + isFromServ + "delimiter" + hasBeenSent + "delimiter" + hopCount;			    			
		    			messages = messages.concat(tweet);
		    			if (!cursorDisaster.isAfterLast()) {
		    				messages = messages.concat("~~");
		    			}
		    		} else break;
		    		}
	    		}
	    		//moving to the next row
	    		cursorDisaster.moveToNext();
	    	} 
	    	//if we are still connected and/or I have updates, send the message
	    	if (mBlueService.getState() == BluetoothComms.STATE_CONNECTED )  {
	    		if ( messages.length() > 35  ) {
	    			if (mBlueService != null)
	    				mBlueService.write(messages.getBytes());  
	    			Log.i(TAG, "TWEETS HAVE BEEN SENT"); 
	    	    } 
	    		else { 	    			
	    			Log.i(TAG, "no new tweets from other peers to be sent ");
	    		}
	    	} 
	    	else {Log.i(TAG, "STATE is NOT CONNECTED "); }
	    	
	    } else {
	    	  //if i don't have any tweet i just send a request to close connection
			  Log.i(TAG,"sending closing request inside send relevant tweets"); 
			  String messageClose = "Close connection";
			  if (mBlueService != null)
				  mBlueService.write(messageClose.getBytes()); 					
	      }
	    }
	    else
	    	getDatabase();
 }
  
 private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){	 
	 
	    public void onReceive(Context arg0, Intent intent) {     
	      int level = intent.getIntExtra("level", 0);	      
	      if (firstBroadcastTime == 0) {
	    	  if (mSettings.contains("firstBroadcastTime")) {
	    		  firstBroadcastTime = mSettings.getLong("firstBroadcastTime", 0);
	    		  Log.i(TAG,"restoring first broadc. time");
	    	  }
	    	  else {
	    		  firstBroadcastTime = new Date().getTime();
	    		  SharedPreferences.Editor editor = mSettings.edit();
	    	      editor.putLong("firstBroadcastTime", firstBroadcastTime);
	    	      editor.commit();
	    	  }
	      }	      		
	      try {	    	  
	    	  long time =  Math.round( ( (new Date().getTime() - firstBroadcastTime) / 1000) )  ;
	    	  batteryWriter.write(time + ":" + level + "\n");	 	    	  	    	 	    	  
	      } 
	      catch (IOException e) {}	     
	    	  
	      if ( prefs.getBoolean("prefDisasterMode", false) == true) { // if the disaster mode is enabled
	    	  
	    	  if (level <=50 && isPartialSavingActive == false) {
	    		  Log.i(TAG,"discovery delay has been doubled");
	    		  DevicesDiscovery.discoveryDelay = (DevicesDiscovery.discoveryDelay *2);
	    		  isPartialSavingActive = true;
	    		  SharedPreferences.Editor editor = mSettings.edit();
	    		  editor.putBoolean("isPartialSavingActive", isPartialSavingActive);
	    		  editor.commit();
	    		  
	    	  }
	    	  else if (level > 50 && isPartialSavingActive == true) {
	    		  Log.i(TAG,"discovery delay has been reduced");
	    		  DevicesDiscovery.discoveryDelay = (DevicesDiscovery.discoveryDelay / 2);
	    		  isPartialSavingActive = false;
	    		  SharedPreferences.Editor editor = mSettings.edit();
	    		  editor.putBoolean("isPartialSavingActive", isPartialSavingActive);
	    		  editor.commit();
	    		  
	    	  }
	    	  
	    	   if (level <= 30 && isFullSavingActive == false) {
	    		  Toast.makeText(DisasterOperations.this, "battery level low, stopping discovery", Toast.LENGTH_SHORT).show(); 
	    		  Log.i(TAG,"battery level low, stopping discovery");
	    		  isFullSavingActive = true;
	    		  SharedPreferences.Editor editor = mSettings.edit();
	    		  editor.putBoolean("isFullSavingActive", isFullSavingActive);
	    		  editor.commit();	    		
	    		  //stopService(new Intent(DisasterOperations.this, DevicesDiscovery.class));  
	    		 // stopService(new Intent(DisasterOperations.this, RandomTweetGenerator.class)); 
	    	  }
	    	   else if (level > 30 && isFullSavingActive == true) {
		    		  Toast.makeText(DisasterOperations.this, "enabling discovery again", Toast.LENGTH_SHORT).show(); 
		    		  Log.i(TAG,"enabling dicovery again discovery");
		    		  isFullSavingActive = false;
		    		  SharedPreferences.Editor editor = mSettings.edit();
		    		  editor.putBoolean("isFullSavingActive", isFullSavingActive);
		    		  editor.commit();	    		
		    		  //startService(new Intent(DisasterOperations.this, DevicesDiscovery.class));  
		    		 // startService(new Intent(DisasterOperations.this, RandomTweetGenerator.class)); 
		    	  }
	      }
	    	  
	    }
	  };
		 
}