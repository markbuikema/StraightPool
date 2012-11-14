package com.markbuikema.straightpool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class PictureRetriever extends AsyncTask<Void, Void, HashMap<String,Object>> {

	private String facebookId;

	public PictureRetriever(String facebookId) {
		this.facebookId = facebookId;
	}

	@Override
	protected HashMap<String,Object> doInBackground(Void... arg0) {
		if (facebookId != null) {
			Bundle bundle = new Bundle();
			bundle.putString("fields", "picture,name");
			try {
				String response = FacebookInstance.get().request(facebookId, bundle);
				JSONObject output = new JSONObject(response);
				String name = output.getString("name");
				JSONObject picture = output.getJSONObject("picture");
				JSONObject data = picture.getJSONObject("data");
				URL url = new URL(data.getString("url"));
				Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

				HashMap<String,Object> map = new HashMap<String,Object>(2);
				map.put("name", name);
				map.put("picture", bmp);
				Log.d("HashMap \"name\"", name);
				Log.d("HashMap \"picture\"", bmp==null?"null":bmp.toString());
				
				return map;
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
		} 
		return null;
	}

}
