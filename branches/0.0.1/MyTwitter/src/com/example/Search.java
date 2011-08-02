package com.example;

import java.util.ArrayList;

import winterwell.jtwitter.Twitter.Status;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;









public class Search extends Activity {
	ListView listSearch ;
	SharedPreferences mSettings ;
	ConnectivityManager connec ;
	ConnectionHelper connHelper ;
	private static final String TAG = "Search";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simplelist);
		
		mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE); 
		 connec =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		 connHelper = new ConnectionHelper(mSettings,connec);
		
		// Get the intent, verify the action and get the query
	    Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	    	
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	                MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE);
	        suggestions.saveRecentQuery(query, null);

	      doMySearch(query);
	    }

	}
	
	private void doMySearch(String query) {	
	
		
		
		if (connHelper.testInternetConnectivity()) {
			if (ConnectionHelper.twitter != null ) {
				try {
					Log.i(TAG,"performing search");
					ConnectionHelper.twitter.setMaxResults(20);
					ArrayList<Status> results = (ArrayList<Status>)ConnectionHelper.twitter.search(query);
					 if (results.size() > 0 ) {	
					
						    CustomArrayAdapter adapter = new CustomArrayAdapter(this,R.layout.row,results);
							listSearch = (ListView) findViewById(R.id.itemList);
							listSearch.setAdapter(adapter);					 
					 } else Toast.makeText(this, "No Results", Toast.LENGTH_LONG).show(); 
				 
				} catch (Exception e) {
			    	  
				       
				      }				
				
			}
		}	else Toast.makeText(this, "No Internet connection", Toast.LENGTH_LONG).show(); 
		
		
	}	

}