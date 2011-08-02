package com.example;

import java.io.IOException;
import java.util.Date;

import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

public class TweetContextActions {
	private Cursor cursorSelected;
	ConnectionHelper connHelper;	
	  SharedPreferences prefs;
	  TweetDbActions dbActions;
	  BluetoothAdapter mBtAdapter;
	
	 private static final int FALSE = 0;
	 private static final int TRUE = 1; 
	  private String username = "";
	  private String text = "";	
	  private static final String TAG = "ContextActions";
	
	 /* public void closeDb() {
		  if (dbActions != null) {
			  dbActions.closeDb();
			  dbActions = null;
		  }
	  } */
	  
	public TweetContextActions(ConnectionHelper connHelper,SharedPreferences prefs) {	
		this.connHelper = connHelper;
		this.prefs = prefs;
		dbActions = new TweetDbActions();
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	
	boolean deleteTweet(long id, boolean isDisaster) {
		try {
			if (ConnectionHelper.twitter != null) {
				ConnectionHelper.twitter.destroyStatus(id);
				dbActions.delete(DbOpenHelper.C_ID + "=" + id,DbOpenHelper.TABLE);			
				return true;
			}
			else 
				return false;
		} catch (Exception ex) {
			if (isDisaster) {
				dbActions.delete(DbOpenHelper.C_ID + "=" + id,DbOpenHelper.TABLE);
				dbActions.updateDisasterTable(id, Timeline.FALSE, Timeline.FALSE);
			}
			return false;
			}
	}
	
	 @SuppressWarnings("deprecation")
	boolean favorite(long id, boolean isShowingFavorites){	
		if (connHelper.testInternetConnectivity()) { 
			extractFromCursor(id , DbOpenHelper.TABLE); 	       
	        Date date = new Date();
	        User user = new User(username);
	        Status status = new Status(user,text,id,date);	        
				try {
					//if i am in the timeline view
			        if (!isShowingFavorites) {
			        	Log.i(TAG,"setting as a favorite");
			        	ConnectionHelper.twitter.setFavorite(status, true); 
			        	dbActions.updateTables(TRUE,isShowingFavorites,status);
			        	return true;
			        }
			        //if I am in the favorite view I can just remove from favorites
			       else {
			        	ConnectionHelper.twitter.setFavorite(status, false); 
			        	dbActions.updateTables(FALSE,isShowingFavorites,status);
			        	return true;
			       }
			        
			        } catch (Exception ex) {
			        	Log.i(TAG,"error setting the favorite");
			        	return false;
			        }        
			        
				 }  else
					 return false;
	}

	@SuppressWarnings("deprecation")
	 boolean retweet(long id, String table) {	
		boolean returnValue= false;
		int hasBeenSent = FALSE;  	
	   
		   extractFromCursor(id,table); 
		   Date date = new Date();
	       User user = new User(username);
	       Status status = new Status(user,text,id,date);
		try {       
	        ConnectionHelper.twitter.retweet(status);                  
	        hasBeenSent = TRUE;
	        Log.i(TAG,"retweet sent");
	        returnValue = true;
	        } catch (Exception ex) {	      	  
	        	Log.i(TAG,"Retweet not posted");
	        }
	        //if disaster mode is enabled...
	   if (prefs.getBoolean("prefDisasterMode", false) == true) { 
		 if (!connHelper.testInternetConnectivity()) {			 
			 extractFromCursor(id,table);        
		 }  
	  	 long created  = cursorSelected.getLong(cursorSelected
	   	        .getColumnIndex(DbOpenHelper.C_CREATED_AT));	   	 
		   dbActions.saveIntoDisasterDb(id,created,new Date().getTime(),text,username,"",
				   TRUE,hasBeenSent, TRUE, 0);
		   dbActions.updateDisasterTable(id, hasBeenSent, Timeline.TRUE);
		   //here i dont need to copy into the timeline table since I am retweeting from there
		   hasBeenSent = FALSE;
		   
		   String mac = mBtAdapter.getAddress();
			long time = new Date().getTime();
			String concatenate = text.concat(" " + username) ; 	
			try {
				if (RandomTweetGenerator.generatorWriter != null)
					RandomTweetGenerator.generatorWriter.write(mac + ":" + username + ":" 
							+ concatenate.hashCode() + ":manual:" + time
							+ ":" + new Date().toString() + "\n");
			} catch (IOException e) {	}		
	  }
	   return returnValue;	   	
	}


	private void extractFromCursor(long id, String table) {	
		     cursorSelected = dbActions.contextMenuQuery(id,table);
		     if (cursorSelected != null) {
		     cursorSelected.moveToFirst();  
		     text = cursorSelected.getString(cursorSelected
		   	        .getColumnIndex(DbOpenHelper.C_TEXT));
		     username = cursorSelected.getString(cursorSelected
		 	        .getColumnIndex(DbOpenHelper.C_USER));	  
		}
	}
	
}
