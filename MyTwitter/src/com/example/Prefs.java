package com.example;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.Twitter.User;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


public class Prefs extends PreferenceActivity{
	
	  SharedPreferences mSettings,prefs;
	  OnSharedPreferenceChangeListener prefListener;
	  ConnectionHelper connHelper;
	  BluetoothAdapter mBluetoothAdapter;
	  EditTextPreference mEditTextUsername;
	  
	  String prova;
	  String userName = "";
	  static final int BLUE_ENABLED = 1 ;
	  static final int DIS_MODE_DISABLED = 3;
	  static final int REQUEST_DISCOVERABLE = 2;
	  
	  static final String DEVICE_MAC = "DEVICE MAC ADDRESS";
	  static final String TAG = "Prefs";	 
	  
  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);    
    addPreferencesFromResource(R.xml.prefs);
    
	  setResult(RESULT_CANCELED);  
    // Get shared preferences
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE); 
   
    // if the username has been already set enable the disaster checkbox
    mEditTextUsername = (EditTextPreference) getPreferenceScreen().findPreference(
            "username");
    mEditTextUsername.setText(mSettings.getString("user", ""));
    if (mEditTextUsername.getText().equals(""))
    	 new GetAuthenticatingUsername().execute();
    
    ConnectivityManager connec =  (ConnectivityManager)getSystemService(
			 Context.CONNECTIVITY_SERVICE);
    connHelper = new ConnectionHelper(mSettings,connec);
    
    prefListener = new OnSharedPreferenceChangeListener() {
    	
        public void onSharedPreferenceChanged(SharedPreferences preferences,
            String key) {
      	 
        if (key.equals("prefDisasterMode") &&  prefs.getBoolean("prefDisasterMode", false) == true ) {
      	 enableBluetooth();         	 
        }
        //to be removed after testing
		  //---------------------------------------------------------------------
        else if (key.equals("username")  ) {        	  
      	  prova = mEditTextUsername.getText();
      	  if (prova != null) {        		  
      		  if (prova.length() != 0  ) {       			 
      			
      			  if (connHelper.testInternetConnectivity()) {
      				  if (ConnectionHelper.twitter != null){       							
      				   try {	
      					 User me = ConnectionHelper.twitter.getUser(prova);
      					 Log.i(TAG,"screen name " + me.getScreenName());
      					 Log.i(TAG,"name " + me.getName());
      					 Toast.makeText(Prefs.this, "Username updated", Toast.LENGTH_SHORT).show();
      					 findPreference("prefDisasterMode").setEnabled(true); 
      				        // TO BE DELETED AFTER TESTING
      	      			  SharedPreferences.Editor editor = mSettings.edit();
      	      			  editor.putString("user", prova);
      	      			  editor.commit();      
      				   }
      				   catch (Exception ex ) {
      					   	 Log.i(TAG,"ERROR");
      				  		 Toast.makeText(Prefs.this, "Insert valid username", Toast.LENGTH_SHORT).show();
      				  		 findPreference("prefDisasterMode").setEnabled(false);
      				  		 
      				   } 
      			      }
      			   } 
      			  else {
      				  Toast.makeText(Prefs.this, "Username updated", Toast.LENGTH_SHORT).show();
      				  // TO BE DELETED AFTER TESTING
          			  SharedPreferences.Editor editor = mSettings.edit();
          			  editor.putString("user", prova);
          			  editor.commit();      
      			  }
      						  
      		  }
      		  else
      			  findPreference("prefDisasterMode").setEnabled(false);
      	  } 
        }
      //-----------------------------------------------------------------------
		  //end part to be removed
        else if (key.equals("prefDisasterMode") &&  prefs.getBoolean("prefDisasterMode", false) == false) {        	    
      	    setResult(DIS_MODE_DISABLED);
				finish();
      	    Log.i(TAG,"disaster mode disabled");

        } 
        
        }
      };
    
    prefs.registerOnSharedPreferenceChangeListener(prefListener);	    
  }
  
  
  private void enableBluetooth() {
	  mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	  if (mBluetoothAdapter == null) {
		  Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
          finish();
	  } else {		  
		  /*
		  if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {		
	            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	            startActivityForResult(discoverableIntent,REQUEST_DISCOVERABLE);           
			  
		  } */ 
		  
		  // TO BE MODIFIED AFTER EXPERIMENTS
		  if (!mBluetoothAdapter.isEnabled()) {
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_DISCOVERABLE);
		  }
		  
		  else {
			  	Intent intent = new Intent();
			  	intent.putExtra(DEVICE_MAC, mBluetoothAdapter.getAddress());
	        	setResult(BLUE_ENABLED,intent);
				finish();
	        }	 
	  }
  }


@Override
protected void onDestroy() {
	 prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
	super.onDestroy();
}


@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	switch(requestCode) {
	case REQUEST_DISCOVERABLE:
		if (resultCode != Activity.RESULT_CANCELED) {
			mBluetoothAdapter.setName(mEditTextUsername.getText()); 			
        	setResult(BLUE_ENABLED);
			finish();
		} else {
			  Log.i(TAG,"discoverability non enabled"); 
			  CheckBoxPreference disasterPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("prefDisasterMode");
			  disasterPreference.setChecked(false);
			  
		}
	}
	
}  
  
class GetAuthenticatingUsername extends AsyncTask<Void, Void, String> {		
	
	@Override
	protected String doInBackground(Void... nil ) {
		try {			
			if (connHelper.testInternetConnectivity()) {				
				  if (ConnectionHelper.twitter != null){					  
					  Twitter.Status status =   ConnectionHelper.twitter.getStatus() ; 
				   	if (status == null) {					   
				   		ConnectionHelper.twitter.setStatus(".");
				   		try {
				   		Thread.sleep(1000);
				   		} catch (InterruptedException ex) {}
				   		status =ConnectionHelper.twitter.getStatus();
				   		if (status != null) {
						   userName = status.getUser().getScreenName();			   
						   Long id = status.getId().longValue();
						   ConnectionHelper.twitter.destroyStatus(id);
						   return userName;
					   }
					   else return "";
				  	}
				 	  else {
					   userName = status.getUser().getScreenName();
					   return userName;
				   		}				   			  
			      	} 
				  else
			      	return "";
			   }  
			else
				return "";
			
		}
		catch (Exception ex){
			Log.i(TAG,"getting username exception");
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

}
