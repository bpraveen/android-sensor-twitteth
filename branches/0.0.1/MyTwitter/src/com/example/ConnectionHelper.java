package com.example;

import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterAccount;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ConnectionHelper {
	SharedPreferences mSettings;
	public String mToken;
	public String mSecret;
	ConnectivityManager connec;
	static Twitter twitter = null;
	public static final String TAG = ConnectionHelper.class.toString();

	ConnectionHelper(SharedPreferences prefs, ConnectivityManager conn) {
		connec = conn;
		mSettings = prefs;

	}

	boolean doLogin() {

		mToken = mSettings.getString(OAUTH.USER_TOKEN, null);
		mSecret = mSettings.getString(OAUTH.USER_SECRET, null);
		if (!(mToken == null || mSecret == null)) {
			OAuthSignpostClient client = new OAuthSignpostClient(
					OAUTH.CONSUMER_KEY, OAUTH.CONSUMER_SECRET, mToken, mSecret);
			if (twitter == null) {
				twitter = new Twitter(null, client);

				try {
					TwitterAccount twitterAcc = new TwitterAccount(twitter);
					twitterAcc.verifyCredentials();
					Log.i(TAG," Returned credentials true" );
					System.out.println(" Returned credentials true");
					return true;

				} catch (Exception ex) {
					System.out.println(" Returned credentials false");
					Log.i(TAG," Returned credentials false" );
					return false;
				}
			} else
				return true;
		}
		return false;
	}

	boolean testInternetConnectivity() {

		if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
				|| // UMTS
				connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING
				|| // WiFi
				connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) {
			System.out.println(" Returned connectivity true");
			Log.i(TAG,"Returned connectivity true");
			return true;
		} else {
			System.out.println(" Returned connectivity false");
			Log.i(TAG,"Returned connectivity false");
			return false;
		}
	}
}
