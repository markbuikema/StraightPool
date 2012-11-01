package com.markbuikema.straightpool;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class FacebookInstance {

	private static Facebook facebook;

	public static Facebook get(final Context context) {
		if (facebook == null) {
			facebook = new Facebook("295512737222106");

//			SharedPreferences prefs = context.getSharedPreferences("settings", 0);
//			String token = prefs.getString("facebook", "0");
//			if (!token.equals("0")) {
//				facebook.setAccessToken(token);
//				facebook.setAccessExpires(0);
//			} else {
//				String[] permissions = new String[] { "user_about_me", "user_birthday", "friends_birthday" };
//
//				facebook.authorize((Activity) context, permissions, new DialogListener() {
//
//					public void onFacebookError(FacebookError e) {
//					}
//
//					public void onError(DialogError e) {
//					}
//
//					public void onComplete(Bundle values) {
//						SharedPreferences settings = context.getSharedPreferences("settings", 0);
//						SharedPreferences.Editor editor = settings.edit();
//						editor.putString("facebook", facebook.getAccessToken());
//						editor.apply();
//					}
//
//					public void onCancel() {
//					}
//				});
//			}
		}
		return facebook;
	}

}
