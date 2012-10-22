package com.markbuikema.straightpool;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

public class FacebookInstance {

	private static Facebook facebook;
	
	public static Facebook get() {
		if (facebook == null) {
			facebook = new Facebook("295512737222106");
		}
		return facebook;
	}

}
