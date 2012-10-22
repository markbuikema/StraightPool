package com.markbuikema.straightpool;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * A convenience class for Authorization with Twitter.
 * 
 * @author Ruud Greven
 * @version 0.1
 */
public class AuthorizationManager {

	public static final String consumerkey = "QOmQeIuM6QP2cM9v8ZIw";
	public static final String consumersecret = "Klyre3GOJzsdfqWVfTizplkAxXEanBNyVEDegfynw";
	public static final String callbackUrl = "app://straightpool";

	private static AuthorizationManager instance;

	private OAuthProvider provider;
	private OAuthConsumer consumer;
	private Context context;

	/**
	 * Use this to get the instance of the AuthorisationManager. Use
	 * setContext(Context context) the first time after this call to set the
	 * basecontext.
	 * 
	 * @return
	 */
	public static AuthorizationManager getInstance() {
		if (instance == null) {
			instance = new AuthorizationManager();
		}
		return instance;
	}

	public AuthorizationManager() {
	}

	public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * Get a consumer object. When a user is logged in the consumer will be
	 * configured with the user key and user secret.
	 * 
	 * @return
	 */
	public OAuthConsumer getConsumer() {
		if (consumer == null) {
			consumer = new CommonsHttpOAuthConsumer(consumerkey, consumersecret);
			SharedPreferences settings = context.getSharedPreferences("authorizationprefs", 0);

			if (settings.contains("user_key")) {
				String userKey = settings.getString("user_key", "");
				String userSecret = settings.getString("user_secret", "");
				consumer.setTokenWithSecret(userKey, userSecret);
			}
		}
		return consumer;
	}

	/**
	 * Get the oAuth provider for Twitter
	 * 
	 * @return
	 */
	public OAuthProvider getProvider() {
		if (provider == null) {
			provider = new DefaultOAuthProvider("http://twitter.com/oauth/request_token",
					"http://twitter.com/oauth/access_token", "http://twitter.com/oauth/authorize");
		}
		return provider;
	}

	/**
	 * Check if a user is already authorized. This method will check if there is
	 * a key named "user_key" in the configuration file
	 * 
	 * @return true if authorization settings are available, false if not
	 */
	public boolean isAuthorized() {
		SharedPreferences settings = context.getSharedPreferences("authorizationprefs", 0);
		return settings.contains("user_key");
	}

	/**
	 * Saves the userKey and userSecret to the configuration file
	 * 
	 * @param userKey
	 * @param userSecret
	 */
	public void saveAuthorisation(String userKey, String userSecret) {
		// Save user_key and user_secret in user preferences and return
		SharedPreferences settings = context.getSharedPreferences("authorizationprefs", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("user_key", userKey);
		editor.putString("user_secret", userSecret);
		editor.commit();
	}

	/**
	 * Removes the current authorization settings form the configuration file
	 */
	public void clearAuthorisation() {
		// Save user_key and user_secret in user preferences and return
		SharedPreferences settings = context.getSharedPreferences("authorizationprefs", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("user_key");
		editor.remove("user_secret");
		editor.commit();
	}
}
