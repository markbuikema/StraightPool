package com.markbuikema.straightpool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class InitializationActivity extends Activity {

	private Facebook facebook;
	private ProfileDatabase db;

	private Button skipButton;
	private Button loginButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_initialization);

		facebook = FacebookInstance.get();
		db = ProfileDatabase.getInstance(this);

		skipButton = (Button) findViewById(R.id.button_skip);
		loginButton = (Button) findViewById(R.id.button_login);

		skipButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setClass(InitializationActivity.this, CreateProfileActivity.class);
				i.putExtra("userprofile", true);
				startActivity(i);
				finish();
			}
		});

		loginButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				facebookLogin();
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		FacebookInstance.get().authorizeCallback(requestCode, resultCode, data);

	}

	private void createProfileFromFacebook() {
		AsyncFacebookRunner runner = new AsyncFacebookRunner(facebook);
		Bundle params = new Bundle();
		params.putString("fields", "first_name,last_name,birthday,picture");
		runner.request("me", params, new RequestListener() {

			public void onMalformedURLException(MalformedURLException e, Object state) {
				Log.d("error", e.getMessage());
			}

			public void onIOException(IOException e, Object state) {
				Log.d("error", e.getMessage());

			}

			public void onFileNotFoundException(FileNotFoundException e, Object state) {
				Log.d("error", e.getMessage());
			}

			public void onFacebookError(FacebookError e, Object state) {
				Log.d("error", e.getMessage());
			}

			public void onComplete(String response, Object state) {
				try {
					JSONObject output = new JSONObject(response);
					String firstName = output.getString("first_name");
					String lastName = output.getString("last_name");
					String fbId = output.getString("id");
					String bdayString = output.getString("birthday");
					final GregorianCalendar bday = new GregorianCalendar();
					if (bdayString != null && bdayString.split("/").length == 3) {
						bday.set(GregorianCalendar.MONTH, Integer.valueOf(bdayString.split("/")[0]) - 1);
						bday.set(GregorianCalendar.DAY_OF_MONTH, Integer.valueOf(bdayString.split("/")[1]));
						bday.set(GregorianCalendar.YEAR, Integer.valueOf(bdayString.split("/")[2]));
					} else {

						AlertDialog.Builder builder = new AlertDialog.Builder(InitializationActivity.this);
						AlertDialog dialog;

						View view = LayoutInflater.from(InitializationActivity.this).inflate(R.layout.dialog_datepicker, null);

						final DatePicker picker = (DatePicker) view.findViewById(R.id.datepicker);

						picker.setMaxDate(bday.getTimeInMillis());
						picker.setMaxDate(new GregorianCalendar().getTimeInMillis());

						dialog = builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {
								bday.set(GregorianCalendar.DAY_OF_MONTH, picker.getDayOfMonth());
								bday.set(GregorianCalendar.MONTH, picker.getMonth());
								bday.set(GregorianCalendar.YEAR, picker.getYear());
								dialog.dismiss();
							}
						}).setCancelable(false).setTitle("Please select your birthday").create();
						dialog.show();
					}

					db.open();
					db.createEntry(firstName, lastName, bday, fbId);
					db.createEntry(firstName, lastName, bday, fbId);
					db.close();

					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setClass(InitializationActivity.this, MainActivity.class);
					startActivity(i);
					finish();

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}

	private void facebookLogin() {
		String[] permissions = new String[] { "user_about_me", "user_birthday", "friends_birthday" };
		facebook.authorize(this, permissions, new DialogListener() {

			public void onComplete(Bundle values) {
				Log.d("FB ACCESS TOKEN ", facebook.getAccessToken());
				SharedPreferences settings = getSharedPreferences("settings", 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("facebook", facebook.getAccessToken());
				editor.apply();

				if (facebook.isSessionValid()) {
					createProfileFromFacebook();
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
