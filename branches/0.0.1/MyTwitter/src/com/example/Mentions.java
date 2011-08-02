package com.example;

import java.util.ArrayList;

import winterwell.jtwitter.Twitter.Status;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.example.Timeline.Retweet;
import com.example.Timeline.SetFavorite;

public class Mentions extends Activity {
	ListView listMentions;
	SharedPreferences mSettings,prefs ;	
	ConnectionHelper connHelper ;
	private static final String TAG = "Favorites";
	boolean isThereConn = false;
	ArrayList<Status> results = null;
	TimelineAdapter adapter;
	Cursor cursor;
	private SQLiteDatabase db;
	private DbOpenHelper dbHelper;
	AlertDialog.Builder alert;
	private EditText input;
	TweetDbActions dbActions;
	TweetContextActions contextActions;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simplelist);
		listMentions = (ListView) findViewById(R.id.itemList);	 		
		input = new EditText(this);
		mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		 dbHelper = new DbOpenHelper(this);
		 db = dbHelper.getWritableDatabase(); 
		 dbActions = new TweetDbActions();
		 connHelper = new ConnectionHelper(mSettings,Timeline.connec);
		 contextActions = new TweetContextActions(connHelper,prefs);
		 cursor = db.query(DbOpenHelper.TABLE_MENTIONS, null, null, null, null, null,
			     DbOpenHelper.C_CREATED_AT + " DESC");
		 if (cursor != null)
			 if ( cursor.getCount() == 0) {
				 finish();
				 Toast.makeText(this, "No mentions to be shown", Toast.LENGTH_SHORT).show();     	
			 }
		 Cursor cursorPictures = db.query(DbOpenHelper.TABLE_PICTURES, null,null, null, null, null, null);
			    // Setup the adapter
		 cursorPictures.moveToFirst();
		adapter = new TimelineAdapter(this, cursor, cursorPictures);
		listMentions.setAdapter(adapter);
		registerForContextMenu(listMentions);
	}

	@Override
	protected void onDestroy() {
		db.close();
		cursor.close();
		super.onDestroy();
		//contextActions.closeDb();
		//contextActions = null;
	//	dbActions.closeDb();
		//dbActions = null;
	}
	
	 @Override
	  public void onCreateContextMenu(ContextMenu menu, View v,
	          ContextMenuInfo menuInfo) {
	      super.onCreateContextMenu(menu, v, menuInfo);
	      menu.add(0, Timeline.REPLY_ID, 0, "Reply");
	      menu.add(0, Timeline.RETWEET_ID, 1, "Retweet");	     
	  }  
	 
	 @Override
	  public boolean onContextItemSelected(MenuItem item) {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

	      switch(item.getItemId()) {
	          case Timeline.REPLY_ID:                                        
	              try {
	                String user = dbActions.userDbQuery(info, DbOpenHelper.TABLE_MENTIONS);              
	                input.setText("@" + user);
	                showDialog(0); 
	                
	              } catch (Exception ex) {}
	             
	              return true;
	          case Timeline.RETWEET_ID: 
	        	  if (connHelper.testInternetConnectivity() || 
	        			  prefs.getBoolean("prefDisasterMode", false) == true ) 
	        		  new Retweet().execute(info.id);     
	        		  
	        	  else 
	        		  Toast.makeText(this, "No internet connectivity", Toast.LENGTH_LONG).show(); 
	         	  return true;       	  
	              	  
	      }
	      return super.onContextItemSelected(item);
	  }  
		 
	 @Override
	 protected Dialog onCreateDialog(int id) {	
	 	
	 	alert = new AlertDialog.Builder(this);
	 	alert.setTitle("Send a Tweet");
	   	
	   	// Set an EditText view to get user input     	
	   	alert.setView(input);  	
	   	alert.setPositiveButton("Send", new OnClickListener() {
	   		public void onClick(DialogInterface dialog, int whichButton) {
	   		 String message = input.getText().toString();	  	
	   		  Timeline.activity.sendMessage(message);
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
		
	
	 class Retweet extends AsyncTask<Long, Void, Boolean> {		
		 ProgressDialog postDialog; 
		
			@Override
			protected void onPreExecute() {
				postDialog = ProgressDialog.show(Mentions.this, 
						"Posting retweet", "Please wait while the retweet is being posted", 
						true,	// indeterminate duration
						false); // not cancel-able
			}
			
			@Override
			protected Boolean doInBackground(Long... id ) {
				return (contextActions.retweet(id[0], DbOpenHelper.TABLE_MENTIONS));					 
			}		

			// This is in the UI thread, so we can mess with the UI
			@Override
			protected void onPostExecute(Boolean result) {
				postDialog.dismiss();
				if (result)
					Toast.makeText(Mentions.this, "Retweet posted succesfully", Toast.LENGTH_SHORT).show();
				else 
					 Toast.makeText(Mentions.this, "Retweet not posted", Toast.LENGTH_SHORT).show(); 
				
			}
		}
	
	
}
