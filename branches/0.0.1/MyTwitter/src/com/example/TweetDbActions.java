package com.example;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.apache.http.util.ByteArrayBuffer;

import winterwell.jtwitter.Twitter.Status;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class TweetDbActions {
	DbOpenHelper dbHelper;
	private SQLiteDatabase db = UpdaterService.db;	
	private Cursor cursorNewPeer,cursorDisaster,cursorSelected, cursorPeers;
	private static final String TAG	= "TweetDbActions";
	
	/*public TweetDbActions() {
		getDatabase();
	}
	/* private void getDatabase() {
			// Initialize DB
		  	  dbHelper = new DbOpenHelper(MyTwitter.activity);
		  	  db = dbHelper.getWritableDatabase();    	  
		  	  
			  String where = DbOpenHelper.C_MET_AT + "<" + (new Date().getTime() - 86400000/2)  ;	   
			  db.delete(DbOpenHelper.TABLE_ADDRESSES,where, null);				
		  } */
	
	public long getTimestampLastTweet(String mConnectedDeviceName ){
	  long recentTweetTime = 0;
	  cursorNewPeer = db.query(DbOpenHelper.TABLE_DISASTER, new String[] {DbOpenHelper.C_ID,
			  DbOpenHelper.C_CREATED_AT}, DbOpenHelper.C_USER + "='" + mConnectedDeviceName + "'", null, null, 
			  null, DbOpenHelper.C_CREATED_AT + " DESC"); 	
	  
	  if (cursorNewPeer.getCount() > 0) {
		  cursorNewPeer.moveToFirst();
		  recentTweetTime = cursorNewPeer.getLong(cursorNewPeer.getColumnIndex(DbOpenHelper.C_CREATED_AT)); 		  
	  }	
	  return recentTweetTime;
	}
	
	Cursor disasterDbQuery(String where) {
		  if (db != null) {
		    	if (db.isOpen()) {
		    		try{
		    			synchronized (this) {
		    				cursorDisaster = db.query(DbOpenHelper.TABLE_DISASTER, null, where, null, null, null,
		    						DbOpenHelper.C_ADDED_AT + " DESC");		    			
		    				return cursorDisaster;
		    			}
		    		} 
		    		catch (Exception ex) { return null; }
		    	} 
		    	else 
		    		return null;
		    	
		    } 
		  else return null;
		  	  
	  }
	
	synchronized void updateTables(int isFavorite,boolean isShowingFavorites, Status status){
		 ContentValues values = new ContentValues();	
		 values.put(DbOpenHelper.C_IS_FAVORITE,isFavorite );
		 db.update(DbOpenHelper.TABLE, values, DbOpenHelper.C_ID + "=" + status.id.longValue() , null);
		 if (isShowingFavorites) 
			 db.delete(DbOpenHelper.TABLE_FAVORITES, DbOpenHelper.C_ID + "=" + status.id.longValue() , null);
		 else {
			 values = DbOpenHelper.statusToContentValues(status);
			 db.insert(DbOpenHelper.TABLE_FAVORITES, null, values);
		 }
		 
	}
	
    void savePairedPeer(String address, int tweetsNumber) {		  			 
		  if (address != null) {			 
			  ContentValues values = new ContentValues();
			  values.put(DbOpenHelper.C_MAC, address );
			  values.put(DbOpenHelper.C_MET_AT, new Date().getTime() );
		  	  values.put(DbOpenHelper.C_TWEETS_NUMBER, tweetsNumber );			    
		  	  try {
		  		 if (db != null)
		  			 db.insertOrThrow(DbOpenHelper.TABLE_ADDRESSES, null, values);  
		    	
		  	  } catch (SQLException ex) {
		  		 if (db != null)
		  			 db.update(DbOpenHelper.TABLE_ADDRESSES, values,
		  					 DbOpenHelper.C_MAC + "='" + address + "'", null);
		    	}
		  }
		  else 
			  Log.i(TAG,"address is null");
	  }
    
    void updateDisasterTable(long id, int hasBeenSent, int isValid) {
    	ContentValues values = new ContentValues();		  
  	    values.put(DbOpenHelper.C_HASBEENSENT, hasBeenSent ); 
  	    values.put(DbOpenHelper.C_IS_VALID, isValid);
    	db.update(DbOpenHelper.TABLE_DISASTER, values, DbOpenHelper.C_ID + "=" + id, null);
    }
    
   boolean saveIntoDisasterDb(long id,long date,long time, String status, String user,
  		  String sentBy, int isFromServer, int hasBeenSent, int isValid, int hopCount) {
  	
  	  ContentValues values = new ContentValues();		  
  	    values.put(DbOpenHelper.C_ID, id ); 	
  	    values.put(DbOpenHelper.C_CREATED_AT, date);	
  	    values.put(DbOpenHelper.C_ADDED_AT, time);
  	    values.put(DbOpenHelper.C_TEXT, status);
  	    values.put(DbOpenHelper.C_USER, user);	 
  	    values.put(DbOpenHelper.C_ISFROMSERVER, isFromServer );  	   
  	    values.put(DbOpenHelper.C_HASBEENSENT, hasBeenSent );	  	   
  	    values.put(DbOpenHelper.C_SENT_BY, sentBy );	
  	    values.put(DbOpenHelper.C_IS_VALID, isValid );
  	    values.put(DbOpenHelper.C_HOPCOUNT, hopCount );
  	  //  Log.i(TAG, "trying to insert into the Db");	 
  	    try {
  	    	synchronized (this) {
  	    		db.insertOrThrow(DbOpenHelper.TABLE_DISASTER, null, values);
  	    		return true;
  	    	}
  	    } 
  	    catch (Exception ex) {  	    	  
  	    	return false;
  	    }
    }
    
    synchronized boolean copyIntoTimelineTable(long id,long created, String status, String user, int isFromServer) {
	    ContentValues values = new ContentValues();
	    values.put(DbOpenHelper.C_ID, id);
	    values.put(DbOpenHelper.C_CREATED_AT, created);
	    values.put(DbOpenHelper.C_TEXT, status);
	    values.put(DbOpenHelper.C_USER, user);
	    values.put(DbOpenHelper.C_IS_DISASTER, Timeline.TRUE);
	    
	    try {
	    	db.insertOrThrow(DbOpenHelper.TABLE, null, values);
	    	return true;
	    } catch (Exception ex) { 
	    	
	    		return false;
	    	   	
	    	}
  }
    
    synchronized String userDbQuery(AdapterContextMenuInfo info, String table) {
    	cursorSelected = db.query(true, table,new String[] {DbOpenHelper.C_USER},
      		  DbOpenHelper.C_ID + "=" + info.id, null, null, null, null, null); 
        if (cursorSelected != null) {
            cursorSelected.moveToFirst();
            try {
            	String user = cursorSelected.getString(cursorSelected
          	        .getColumnIndexOrThrow(DbOpenHelper.C_USER));
            	return user;
            }
            catch (Exception ex) {
            	return ""; 
            }            	
        }
        else
        	return "";    
        
    }
    
    synchronized Cursor contextMenuQuery(long id, String table) {
    	
		 cursorSelected = null;
		 cursorSelected = db.query(true, table, new String[] {DbOpenHelper.C_TEXT, DbOpenHelper.C_USER,
				 DbOpenHelper.C_CREATED_AT}, 
		   		  DbOpenHelper.C_ID + "=" + id, null, null, null, null, null); 
		 return cursorSelected;
    }
    
    Cursor peersDbQuery(String deviceMac) {	
		 if (db != null) {
		    if (db.isOpen()) {
		    	try{
		    		cursorPeers = db.query(DbOpenHelper.TABLE_ADDRESSES,new String[] {DbOpenHelper.C_MAC,
		    				DbOpenHelper.C_MET_AT,DbOpenHelper.C_TWEETS_NUMBER},
		    				DbOpenHelper.C_MAC + "='" + deviceMac + "'", null, null, null, null, null); 		    		  
		    		return cursorPeers; 
		    			
		    		} catch (Exception ex) {  		    			
		    		    return null;
		    		 }		    		
		    	} else {	
		    		return null;
		    	}
		    } else {		    	
		    	return null;	  
	 }    
 }    
     void delete(String where,String table){
    	 if (db != null) {
    		 synchronized(this) {
    			 if (db != null) {
    				 if (db.delete(table, where, null) > 0)
    					 Log.i(TAG,"tweet deleted");
    			 }
    		 }	
    	 }
    }
    private class FetchProfilePic implements Runnable {
    	Status status;
    	
    	FetchProfilePic(Status status) {
    		this.status = status;
    	}
    	
    	public void run() {
    		try {
         		Log.d(TAG,"creating image");
    			URL url = new URL(status.getUser().getProfileImageUrl().toURL().toString());
    			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    			connection.setDoInput(true);
    			connection.connect();
    			InputStream input = connection.getInputStream();			
    			//Drawable d = Drawable.createFromStream(input, "src");
    			
    			BufferedInputStream bis = new BufferedInputStream(input,128);			
    			ByteArrayBuffer baf = new ByteArrayBuffer(128);			
    			//get the bytes one by one			
    			int current = 0;			
    			while ((current = bis.read()) != -1) {			
    			        baf.append((byte) current);			
    			}		
    			ContentValues dataToInsert = new ContentValues(); 		
    			dataToInsert.put(DbOpenHelper.C_IMAGE,baf.toByteArray());	
    			dataToInsert.put(DbOpenHelper.C_USER,status.getUser().getScreenName());
    			try {
    				synchronized(this) {
    					db.insertOrThrow(DbOpenHelper.TABLE_PICTURES, null, dataToInsert);
    					Timeline.activity.sendBroadcast(new Intent(UpdaterService.ACTION_NEW_TWITTER_STATUS));
    				}
    			} catch (SQLException ex) {}
    			
    		} catch (Exception e) {	
    			Log.e(TAG,"error creating image",e);
    		}
    	}
    }
      boolean insertIntoTimelineTable(Status status) {
    	 int affected2 = 0;
    	 int affected= 0;
     	ContentValues values = DbOpenHelper.statusToContentValues(status);
     	new Thread(new FetchProfilePic(status)).start();
		
         // Insert will throw exceptions for duplicate IDs                	
       	if (status.isFavorite()) {        	
       		try {
       			synchronized(this) {
       				db.insertOrThrow(DbOpenHelper.TABLE_FAVORITES, null, values);
       			}
       		} catch (SQLException ex) {}
           	values.put(DbOpenHelper.C_IS_FAVORITE,1);
       	}
           else 
           	values.put(DbOpenHelper.C_IS_FAVORITE,0);
       	
       	String dot = status.getText();
       	if (!dot.equals(".")) { 
       		String msg = status.getText() + " " + status.getUser().getScreenName();
       		synchronized(this) {
       			affected = db.delete(DbOpenHelper.TABLE,DbOpenHelper.C_ID + "=" + msg.hashCode(), null);     
       			try {
       				affected2 = db.delete(DbOpenHelper.TABLE,DbOpenHelper.C_TEXT + "='" + status.getText() + "'" , null);  
       			} catch (Exception ex) {}
    	
       			try {
       				db.insertOrThrow(DbOpenHelper.TABLE, null, values);           
       			} 
       			catch (SQLException ex) {
       				return false;
     			} 
       		}
     		if (affected > 0 || affected2 > 0 )    			
          	 return false; // we dont have new status
     		else
     			return true; //we have new status
        
       	}
       	else 
       		return false;
     }
   
}
