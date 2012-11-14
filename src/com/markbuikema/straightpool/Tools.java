package com.markbuikema.straightpool;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Tools {

	public static String getContent(String url) {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		
		HttpResponse response = null;
		String stringOutput = "ErrorOutput";

		try {
			response = client.execute(request);
			HttpEntity resEntityGet;
			if (response.getStatusLine().getStatusCode() == 200) {
				resEntityGet = response.getEntity();
				stringOutput = EntityUtils.toString(resEntityGet);
			} else {
				stringOutput = "Status: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase();
			}

		} catch (IOException e) {

		}
		return stringOutput;
	}

	public static Bitmap getBitmap(String url) {
		Bitmap bmp = null;
		try {
			URL imageUrl = new URL(url);
			URLConnection conn = imageUrl.openConnection();
			conn.connect();
			bmp = BitmapFactory.decodeStream(new BufferedInputStream(conn.getInputStream()));
		} catch (Exception e) {
			Log.e("Image failure!", "Fout bij ophalen image");
		}
		return bmp;
	}
}
