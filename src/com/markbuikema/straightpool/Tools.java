package com.markbuikema.straightpool;

import java.io.IOException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

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
}
