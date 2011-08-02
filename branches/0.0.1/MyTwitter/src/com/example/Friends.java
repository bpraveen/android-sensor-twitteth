package com.example;

import java.util.List;

import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.Twitter.User;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;






public class Friends extends Activity{
	ListView listFriends;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simplelist);
		
		setTitle("Friends");
		if (ConnectionHelper.twitter != null) {
		try {
			List<User> friendsList= ConnectionHelper.twitter.getFriends();
			Object[] friendsArr=friendsList.toArray();
	
			String[] stringArray = new String[friendsArr.length];
			for(int i=0; i<friendsArr.length; i++) {
				stringArray[i] = friendsArr[i].toString();
			}
		
			listFriends = (ListView) findViewById(R.id.itemList);
			listFriends.setAdapter(new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1,stringArray));
		} catch (TwitterException ex) {}
		   }
	}
	
	

}
