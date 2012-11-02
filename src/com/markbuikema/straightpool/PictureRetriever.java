package com.markbuikema.straightpool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

public class PictureRetriever extends AsyncTask<Void, Void, Bitmap> {

	private String facebookId;
	private String twitterId;

	public PictureRetriever(String facebookId, String twitterId) {
		this.facebookId = facebookId;
		this.twitterId = twitterId;
	}

	@Override
	protected Bitmap doInBackground(Void... arg0) {
		if (facebookId != null) {
			Bundle bundle = new Bundle();
			bundle.putString("fields", "picture");
			try {
				String response = FacebookInstance.get().request(facebookId, bundle);
				JSONObject output = new JSONObject(response);
				JSONObject picture = output.getJSONObject("picture");
				JSONObject data = picture.getJSONObject("data");
				URL url = new URL(data.getString("url"));
				return BitmapFactory.decodeStream(url.openConnection().getInputStream());

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (twitterId != null) {
			try {
				URL twitterUrl = new URL("https://api.twitter.com/1/users/profile_image/" + twitterId + ".json?size=normal");
				return BitmapFactory.decodeStream(twitterUrl.openConnection().getInputStream());

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
