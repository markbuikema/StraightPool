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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TwitterAuthActivity extends Activity {

	private WebView view;
	private String url;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		url = getIntent().getExtras().getString("uri");

		view = new WebView(this);
		setContentView(view);
		view.loadUrl(url);
		view.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (!url.startsWith("app://straightpool")) {
					view.loadUrl(url);
				} else {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(i);
					finish();
				}
				return true;
			}
		});
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

	public static String getContent(String url) {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		try {
			AuthorizationManager.getInstance().getConsumer().sign(request);
		} catch (OAuthMessageSignerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (OAuthExpectationFailedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (OAuthCommunicationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
}
