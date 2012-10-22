package com.markbuikema.straightpool;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
}
