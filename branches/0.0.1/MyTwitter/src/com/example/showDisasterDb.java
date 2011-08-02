package com.example;

import java.util.Calendar;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;



public class showDisasterDb extends Activity{
	
	  ListView listTimeline;  
	   
	  DbOpenHelper dbHelper;
	  SQLiteDatabase db ;
	  Cursor cursor;
	  TimelineAdapter adapter;
	  Calendar cal = Calendar.getInstance(); 
	  private static final String TAG = "showDisasterDb";
	  int order = 0;
	  TwitterStatusReceiver twitterStatusReceiver;
	  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simplelist);			
		 // Find views by id
		listTimeline = (ListView) findViewById(R.id.itemList);
		
		dbHelper = new DbOpenHelper(this);
		db = dbHelper.getWritableDatabase(); 	  		  	  

		// Get the data from the DB
		cursor = db.query(DbOpenHelper.TABLE_DISASTER, null, DbOpenHelper.C_IS_VALID + "=" + Timeline.TRUE,
			  null, null, null, DbOpenHelper.C_CREATED_AT + " DESC");	  
		startManagingCursor(cursor);  
		    
		 // Setup the adapter		    
		Cursor cursorPictures = db.query(DbOpenHelper.TABLE_PICTURES, null,null, null, null, null, null);
		 cursorPictures.moveToFirst();
		adapter = new TimelineAdapter(this, cursor, cursorPictures);    
		listTimeline.setAdapter(adapter);   
			     
		twitterStatusReceiver = new TwitterStatusReceiver();
		registerReceiver(twitterStatusReceiver, new IntentFilter(Timeline.ACTION_NEW_DISASTER_TWEET));
		
	}
	
	private void cancelNotification() {
		 NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		 notificationManager.cancel(Timeline.NOTIFICATION_ID);
	}

	@Override
	  public void onResume() {
	    super.onResume();
	    // Cancel notification
	   cancelNotification(); 
	       
	  }

	  @Override
	  public boolean onCreateOptionsMenu(Menu menu){
		  super.onCreateOptionsMenu(menu);
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_disaster_db, menu);
	    return true;
	  }
	    
	  
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.menu_clear:
			//db.delete(DbOpenHelper.TABLE_DISASTER, null, null);
			//db.delete(DbOpenHelper.TABLE_ADDRESSES, null, null);
			//db.delete(DbOpenHelper.TABLE,DbOpenHelper.C_IS_DISASTER + "=" + Timeline.TRUE, null);
			cursor.requery();
			return true;
		case R.id.menu_changeOrder:
			if (order == 0) {
				cursor = db.query(DbOpenHelper.TABLE_DISASTER, null, null, null, null, null,
				        DbOpenHelper.C_ADDED_AT + " DESC");
				order = 1;
				Cursor cursorPictures = db.query(DbOpenHelper.TABLE_PICTURES, null,null, null, null, null, null);
				adapter = new TimelineAdapter(this, cursor, cursorPictures);    				
			} else {
				cursor = db.query(DbOpenHelper.TABLE_DISASTER, null, null, null, null, null,
				        DbOpenHelper.C_CREATED_AT + " DESC");
				order = 0;
				Cursor cursorPictures = db.query(DbOpenHelper.TABLE_PICTURES, null,null, null, null, null, null);
				adapter = new TimelineAdapter(this, cursor, cursorPictures);    				
			}
			listTimeline.setAdapter(adapter);
				
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		cursor.close();
		db.close();
		unregisterReceiver(twitterStatusReceiver);
	}
	
	class TwitterStatusReceiver extends BroadcastReceiver {
		  
		@Override
		  public void onReceive(Context context, Intent intent) {		   
		    	cursor.requery();
		  }
		}
	  
	  
	
	
}
