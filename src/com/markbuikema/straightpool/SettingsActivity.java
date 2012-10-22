package com.markbuikema.straightpool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class SettingsActivity extends Activity {

	private Facebook facebook;
	private AsyncFacebookRunner asyncFacebookRunner;
	private AuthorizationManager am;

	private ListView settingsList;
	private ArrayList<SettingType> settings;
	private SettingAdapter adapter;
	private OAuthConsumer consumer;
	private OAuthProvider provider;
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
		am = AuthorizationManager.getInstance();
		am.setContext(this);

		Intent intent = null;
		Uri uri = null;

		if (getIntent() != null) {
			intent = this.getIntent();
			if (intent.getData() != null) {
				uri = intent.getData();

			}
		}

		if (intent != null) {
			if (uri != null
					&& uri.toString().startsWith(
							AuthorizationManager.callbackUrl)) {
				Log.d("Uri", uri.toString());

				if (uri.toString().contains("denied")) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							SettingsActivity.this);
					AlertDialog dialog;
					builder.setMessage(
							"Je moet de app authoriseren om gebruik te kunnen maken van de Twitter functies.")
							.setPositiveButton("OK",
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									}).setCancelable(false);
					dialog = builder.create();
					dialog.show();
				} else {

					String verifier = uri
							.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					GetFromAuthorization async = new GetFromAuthorization();
					async.execute(verifier);

				}
			}
		}

		consumer = am.getConsumer();
		provider = am.getProvider();

		asyncFacebookRunner = new AsyncFacebookRunner(facebook);
		settingsList = (ListView) findViewById(R.id.listview_settings);
		settings = new ArrayList<SettingType>();
		settings.add(new ToggleSetting("Automatically save games", "autosave"));
		settings.add(new FacebookSetting("Facebook login", "facebook"));
		settings.add(new TwitterSetting("Twitter login", "twitter"));
		adapter = new SettingAdapter(this, 0, settings);
		settingsList.setAdapter(adapter);
		settingsList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				adapter.getItem(arg2).performAction();
			}
		});
	}

	public class GetFromAuthorization extends AsyncTask<String, Void, Void> {

		/**
		 * checkt of hij van de authorization activity komt bij het maken van
		 * deze activity
		 * 
		 */
		@Override
		protected Void doInBackground(String... params) {
			try {
				provider.retrieveAccessToken(consumer, params[0]);
			} catch (OAuthMessageSignerException e) {
			} catch (OAuthNotAuthorizedException e) {
			} catch (OAuthExpectationFailedException e) {
			} catch (OAuthCommunicationException e) {
			}

			userKey = consumer.getToken();
			userSecret = consumer.getTokenSecret();

			am.saveAuthorisation(userKey, userSecret);

			return null;
		}

		@Override
		protected void onPostExecute(Void v) {

			settings.get(2).saveState();

		}
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
			view = LayoutInflater.from(SettingsActivity.this).inflate(
					R.layout.list_item_setting, null);
			toggle = new Switch(SettingsActivity.this);
			toggle.setFocusable(false);
			SharedPreferences prefs = SettingsActivity.this
					.getSharedPreferences("settings", 0);
			toggle.setChecked(prefs.getBoolean(key, false));
			((ViewGroup) view).addView(toggle);
			TextView titleView = (TextView) view
					.findViewById(R.id.textview_setting);
			titleView.setText(title);
			this.key = key;
			toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					saveState();
				}
			});
		}

		public void performAction() {
			toggle.setChecked(!toggle.isChecked());
		}

		public void saveState() {
			SharedPreferences prefs = SettingsActivity.this
					.getSharedPreferences("settings", 0);
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
			view = LayoutInflater.from(SettingsActivity.this).inflate(
					R.layout.list_item_setting, null);
			TextView titleView = (TextView) view
					.findViewById(R.id.textview_setting);
			titleView.setText(title);
			LayoutParams iconParams = new LayoutParams(0,
					LayoutParams.MATCH_PARENT);
			iconParams.width = iconParams.height;

			icon = new ImageView(SettingsActivity.this);
			icon.setImageBitmap(BitmapFactory.decodeResource(getResources(),
					R.drawable.ic_facebook));
			icon.setFocusable(false);
			icon.setLayoutParams(iconParams);
			loginDetails = new TextView(SettingsActivity.this);
			loginDetails.setText("Logged in");
			saveState();

			((ViewGroup) view).addView(icon);
			((ViewGroup) view).addView(loginDetails);

		}

		public void performAction() {
			if (!facebook.isSessionValid()) {
				facebookLogin();
			} else {
				new Logout().execute();
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

		private class UsernameRetriever extends AsyncTask<Void, Void, String> {

			@Override
			protected String doInBackground(Void... params) {
				String json = "";
				String username = "";
				try {
					json = facebook.request("me");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					Log.d("response", json);
					JSONObject obj = new JSONObject(json);
					username = obj.getString("name");

				} catch (JSONException e) {
					e.printStackTrace();
				}

				return username;
			}

			public void onPostExecute(String s) {
				loginDetails.setText("Logged in as " + s);
			}
		}

		private void facebookLogin() {
			if (!facebook.isSessionValid()) {
				String[] permissions = new String[] { "user_about_me" };
				facebook.authorize(SettingsActivity.this, permissions,
						new DialogListener() {

							public void onComplete(Bundle values) {
								Log.d("FB ACCESS TOKEN ",
										facebook.getAccessToken());
								SharedPreferences settings = getSharedPreferences(
										"settings", 0);
								SharedPreferences.Editor editor = settings
										.edit();
								editor.putString("facebook",
										facebook.getAccessToken());
								editor.apply();
								new UsernameRetriever().execute();
								saveState();

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
			SharedPreferences prefs = SettingsActivity.this
					.getSharedPreferences("settings", 0);
			SharedPreferences.Editor editor = prefs.edit();

			if (facebook.isSessionValid()) {
				editor.putString(key, facebook.getAccessToken());
				icon.setVisibility(View.GONE);
				loginDetails.setVisibility(View.VISIBLE);
			} else {
				editor.putString(key, "unauthorized");
				icon.setVisibility(View.VISIBLE);
				loginDetails.setVisibility(View.GONE);
			}

			editor.apply();

			Toast.makeText(
					SettingsActivity.this,
					"Facebook state saved: "
							+ prefs.getString(key, "undefined"),
					Toast.LENGTH_LONG).show();

		}

	}

	private class TwitterSetting extends SettingType {

		ImageView icon;
		TextView loginDetails;

		public TwitterSetting(String title, String key) {
			view = LayoutInflater.from(SettingsActivity.this).inflate(
					R.layout.list_item_setting, null);
			TextView titleView = (TextView) view
					.findViewById(R.id.textview_setting);
			titleView.setText(title);
			LayoutParams iconParams = new LayoutParams(0,
					LayoutParams.MATCH_PARENT);
			iconParams.width = iconParams.height;

			icon = new ImageView(SettingsActivity.this);
			icon.setImageBitmap(BitmapFactory.decodeResource(getResources(),
					R.drawable.ic_twitter));
			icon.setFocusable(false);
			icon.setLayoutParams(iconParams);
			loginDetails = new TextView(SettingsActivity.this);
			loginDetails.setText("Logged in");
			saveState();

			((ViewGroup) view).addView(icon);
			((ViewGroup) view).addView(loginDetails);
			this.key = key;
		}

		public void performAction() {
			if (am.isAuthorized()) {
				am.clearAuthorisation();
			} else {
				new Authorize().execute();
			}
			saveState();
		}

		public void saveState() {
			SharedPreferences prefs = SettingsActivity.this
					.getSharedPreferences("settings", 0);
			SharedPreferences.Editor editor = prefs.edit();

			if (am.isAuthorized()) {
				editor.putString(key, am.getConsumer().getToken());
				icon.setVisibility(View.GONE);
				loginDetails.setVisibility(View.VISIBLE);
			} else {
				editor.putString(key, "unauthorized");
				icon.setVisibility(View.VISIBLE);
				loginDetails.setVisibility(View.GONE);
			}

			editor.apply();

			Toast.makeText(
					SettingsActivity.this,
					"Twitter state saved: " + prefs.getString(key, "undefined"),
					Toast.LENGTH_LONG).show();
		}

		private class Authorize extends AsyncTask<Void, Void, String> {

			/**
			 * Logt in en stuurt de user naar de user profile wanneer het klaar
			 * is
			 */

			@Override
			protected String doInBackground(Void... arg0) {
				try {
					return am.getProvider().retrieveRequestToken(
							am.getConsumer(),
							AuthorizationManager.callbackUrl);
				} catch (OAuthMessageSignerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthNotAuthorizedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthExpectationFailedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthCommunicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;

			}

			protected void onPostExecute(String arg0) {

				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setClass(SettingsActivity.this, TwitterAuthActivity.class);
				i.putExtra("uri", arg0);
				startActivity(i);
			}
		}

	}

	private class SettingAdapter extends ArrayAdapter<SettingType> {

		public SettingAdapter(Context context, int textViewResourceId,
				List<SettingType> objects) {
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
