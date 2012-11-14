package com.markbuikema.straightpool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class SettingsActivity extends Activity {

	private Facebook facebook;

	private ListView settingsList;
	private ArrayList<SettingType> settings;
	private SettingAdapter adapter;
	private String userKey;
	private String userSecret;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		FacebookInstance.get().authorizeCallback(requestCode, resultCode, data);
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_settings);
		super.onCreate(savedInstanceState);
		facebook = FacebookInstance.get();

		SharedPreferences prefs = SettingsActivity.this.getSharedPreferences("settings", 0);

		String tokenCheck = prefs.getString("facebook", "unauthorized");
		if (!tokenCheck.equals("unauthorized")) {
			facebook.setAccessToken(tokenCheck);
			facebook.setAccessExpires(0);
		}

		settingsList = (ListView) findViewById(R.id.listview_settings);
		settings = new ArrayList<SettingType>();
		settings.add(new ToggleSetting("Automatically save games", "autosave"));
		settings.add(new ToggleSetting("Keep screen on", "wakelock"));
		settings.add(new FacebookSetting("Facebook login", "facebook"));
		adapter = new SettingAdapter(this, 0, settings);
		settingsList.setAdapter(adapter);
		settingsList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				adapter.getItem(arg2).performAction();
			}
		});

		if (getIntent().getBooleanExtra("from_profilemanager", false)) {
			getFacebookSetting().facebookLogin();
		}
	}
	
	private FacebookSetting getFacebookSetting() {
		for (SettingType setting: settings) {
			if (setting instanceof FacebookSetting) {
				return (FacebookSetting)setting;
			}
		}
		return null;
	}



	private interface Setting {
		public void performAction();

		public void saveState();
	}

	private abstract class SettingType implements Setting {
		protected View view;
		protected String key;

		public View getView() {
			return view;
		}
	}

	@SuppressLint("NewApi")
	private class ToggleSetting extends SettingType {

		private Switch toggle;

		public ToggleSetting(String title, String key) {
			view = LayoutInflater.from(SettingsActivity.this).inflate(R.layout.list_item_setting, null);
			toggle = new Switch(SettingsActivity.this);
			toggle.setFocusable(false);

			LayoutParams toggleParams = new LayoutParams(LayoutParams.WRAP_CONTENT, 64);

			toggle.setLayoutParams(toggleParams);

			SharedPreferences prefs = SettingsActivity.this.getSharedPreferences("settings", 0);
			toggle.setChecked(prefs.getBoolean(key, false));
			((ViewGroup) view).addView(toggle);
			TextView titleView = (TextView) view.findViewById(R.id.textview_setting);
			titleView.setText(title);
			this.key = key;
			toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					saveState();
				}
			});
		}

		public void performAction() {
			toggle.setChecked(!toggle.isChecked());
		}

		public void saveState() {
			SharedPreferences prefs = SettingsActivity.this.getSharedPreferences("settings", 0);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(key, toggle.isChecked());
			editor.apply();
		}

	}

	private class FacebookSetting extends SettingType {
		private ImageView icon;
		private TextView loginDetails;

		public FacebookSetting(String title, String key) {
			this.key = key;
			view = LayoutInflater.from(SettingsActivity.this).inflate(R.layout.list_item_setting, null);
			TextView titleView = (TextView) view.findViewById(R.id.textview_setting);
			titleView.setText(title);
			LayoutParams iconParams = new LayoutParams(64, 64);

			icon = new ImageView(SettingsActivity.this);
			icon.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_facebook));
			icon.setLayoutParams(iconParams);

			loginDetails = new TextView(SettingsActivity.this);

			icon.setFocusable(false);
			loginDetails.setFocusable(false);
			titleView.setFocusable(false);

			loginDetails.setPadding(0, 0, (int) SettingsActivity.this.getResources().getDimension(R.dimen.default_margin), 0);

			((ViewGroup) view).addView(loginDetails);
			((ViewGroup) view).addView(icon);

			saveState();

		}

		public void performAction() {
			if (!facebook.isSessionValid()) {
				facebookLogin();
			} else {
				new Logout().execute();
				SharedPreferences prefs = SettingsActivity.this.getSharedPreferences("settings", 0);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("facebook_user", "unauthorized");
				editor.putString(key, "unauthorized");
				editor.apply();
			}

		}

		private class Logout extends AsyncTask<Void, Void, Void> {

			@Override
			protected Void doInBackground(Void... arg0) {
				try {
					facebook.logout(SettingsActivity.this);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}

			public void onPostExecute(Void v) {

				saveState();
			}

		}

		private class InitializeUserInfo extends AsyncTask<Void, Void, String> {

			Bitmap bitmap;
			String id;

			@Override
			protected String doInBackground(Void... params) {
				String json = "";
				String username = "";
				try {
					json = facebook.request("me");

					URL url = new URL("https://graph.facebook.com/me/picture?type=square&method=GET&access_token=" + facebook.getAccessToken());
					bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					Log.d("response", json);
					JSONObject obj = new JSONObject(json);
					username = obj.getString("name");

					id = obj.getString("id");
				} catch (JSONException e) {
					e.printStackTrace();
				}

				return username;
			}

			public void onPostExecute(String s) {
				loginDetails.setText(s);
				icon.setImageBitmap(bitmap);

				SharedPreferences prefs = SettingsActivity.this.getSharedPreferences("settings", 0);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("facebook_user", id);
				editor.apply();
			}
		}

		private void facebookLogin() {
			if (!facebook.isSessionValid()) {
				String[] permissions = new String[] { "user_about_me", "user_birthday", "friends_birthday" };
				facebook.authorize(SettingsActivity.this, permissions, new DialogListener() {

					public void onComplete(Bundle values) {
						Log.d("FB ACCESS TOKEN ", facebook.getAccessToken());
						SharedPreferences settings = getSharedPreferences("settings", 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putString("facebook", facebook.getAccessToken());
						editor.apply();
						saveState();
						
						if (getIntent().getBooleanExtra("from_profilemanager", false)) {
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setClass(SettingsActivity.this, ProfileManagerActivity.class);
							startActivity(i);
							finish();
						}

					}

					public void onFacebookError(FacebookError error) {
					}

					public void onError(DialogError e) {
					}

					public void onCancel() {
					}
				});
			}
		}

		public void saveState() {
			SharedPreferences prefs = SettingsActivity.this.getSharedPreferences("settings", 0);
			SharedPreferences.Editor editor = prefs.edit();

			if (facebook.isSessionValid()) {
				editor.putString(key, facebook.getAccessToken());
				Log.d("FB ACCESS TOKEN", facebook.getAccessToken());
			} else {
				editor.putString(key, "unauthorized");
			}

			editor.apply();
			icon.setImageBitmap(BitmapFactory.decodeResource(SettingsActivity.this.getResources(), R.drawable.ic_facebook));
			if (facebook.isSessionValid()) {
				loginDetails.setText("Loading...");
				new InitializeUserInfo().execute();
			} else {
				loginDetails.setText("");
			}

			Toast.makeText(SettingsActivity.this, "Facebook state saved: " + prefs.getString(key, "unauthorized"), Toast.LENGTH_SHORT).show();

		}

	}



	private class SettingAdapter extends ArrayAdapter<SettingType> {

		public SettingAdapter(Context context, int textViewResourceId, List<SettingType> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int p, View view, ViewGroup group) {
			return getItem(p).getView();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int pos) {
			return true;
		}

	}
}
