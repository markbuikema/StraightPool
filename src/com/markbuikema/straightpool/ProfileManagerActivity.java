package com.markbuikema.straightpool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;

public class ProfileManagerActivity extends Activity {

	public static final String KEY_FIRSTNAME = "firstname";
	public static final String KEY_LASTNAME = "lastname";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_BIRTHDATE = "birthdate";
	public static final String KEY_PICTURE_URL = "pic_url";
	public static final String KEY_FACEBOOK_ID = "fbid";
	public static final String KEY_TWITTER_ID = "twitterid";

	private GridView profileList;
	private ProfileAdapter adapter;
	private ProfileDatabase db;
	private Facebook facebook;
	private Handler mHandler;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_manager);
		db = ProfileDatabase.getInstance(this);
		facebook = FacebookInstance.get(this);
		mHandler = new Handler(getMainLooper());

		profileList = (GridView) findViewById(R.id.gridview_profiles);
		adapter = new ProfileAdapter(this, 0);
		profileList.setAdapter(adapter);
		profileList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				Profile profile = adapter.getItem(arg2);
				i.putExtra("profileId", profile.getId());
				i.putExtra("facebookId", profile.getFacebookId());
				i.putExtra("twitterId", profile.getTwitterId());
				i.putExtra("first", profile.getFirstName());
				i.putExtra("last", profile.getLastName());
				i.putExtra("bday", profile.getBirthday());
				i.putExtra("url", profile.getPictureUrl());
				i.setClass(ProfileManagerActivity.this, CreateProfileActivity.class);
				startActivity(i);
			}
		});
	}

	public void onResume() {
		super.onResume();
		db.open();
		new ProfilePopulator().execute(db.fetchAllEntries());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.activity_profile_manager, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_profile:
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setClass(this, CreateProfileActivity.class);
			startActivity(i);
			break;
		}
		return false;
	}

	private class ProfilePopulator extends AsyncTask<Cursor, Profile, Void> {

		@Override
		protected Void doInBackground(Cursor... params) {
			Cursor profiles = params[0];
			profiles.moveToFirst();
			while (profiles.moveToNext()) {
				GregorianCalendar birthday = new GregorianCalendar();
				String bday = profiles.getString(profiles.getColumnIndex(KEY_BIRTHDATE));
				int day = Integer.valueOf(bday.split("-")[0]);
				int month = Integer.valueOf(bday.split("-")[1]);
				int year = Integer.valueOf(bday.split("-")[2]);
				birthday.set(year, month, day);

				URL url = null;
				Bitmap picture = null;
				try {
					url = new URL(profiles.getString(profiles.getColumnIndex(KEY_PICTURE_URL)));
					picture = BitmapFactory.decodeStream(url.openConnection().getInputStream());

				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				publishProgress(new Profile(profiles.getString(profiles.getColumnIndex(KEY_ROWID)),
						profiles.getString(profiles.getColumnIndex(KEY_FIRSTNAME)), profiles.getString(profiles.getColumnIndex(KEY_LASTNAME)), birthday,
						profiles.getString(profiles.getColumnIndex(KEY_PICTURE_URL)), picture, profiles.getString(profiles.getColumnIndex(KEY_FACEBOOK_ID)),
						profiles.getString(profiles.getColumnIndex(KEY_TWITTER_ID))));
			}
			profiles.close();
			return null;
		}

		@Override
		public void onPreExecute() {
			adapter.clear();
		}

		@Override
		protected void onProgressUpdate(Profile... p) {
			for (Profile profile : p) {
				adapter.add(profile);
			}
		}

		@Override
		public void onPostExecute(Void v) {
			db.close();
			if (adapter.getCount() == 0) {
				createUserProfile();
			}
		}

	}

	private void createUserProfile() {
		Log.d("SESSIONVALID:", facebook.isSessionValid() + "");
		if (facebook.isSessionValid()) {
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
						String id = output.getString("id");
						String bdayString = output.getString("birthday");
						GregorianCalendar bday = new GregorianCalendar();
						if (bdayString.split("/").length == 3) {
							bday.set(GregorianCalendar.MONTH, Integer.valueOf(bdayString.split("/")[0]) - 1);
							bday.set(GregorianCalendar.DAY_OF_MONTH, Integer.valueOf(bdayString.split("/")[1]));
							bday.set(GregorianCalendar.YEAR, Integer.valueOf(bdayString.split("/")[2]));
						} else {
							bday = null;
						}
						JSONObject picture = output.getJSONObject("picture");
						JSONObject data = picture.getJSONObject("data");
						String url = data.getString("url");

						db.open();
						db.createEntry(firstName, lastName, bday, url, id, null);
						db.close();

						mHandler.post(new Runnable() {

							public void run() {
								db.open();
								new ProfilePopulator().execute(db.fetchAllEntries());

							}

						});

					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});
		} else {
			createUserProfileManually();
		}
	}

	private void createUserProfileManually() {
		AlertDialog.Builder builder = new AlertDialog.Builder(ProfileManagerActivity.this);
		AlertDialog dialog;
		dialog = builder.setTitle("User profile")
				.setMessage("You must create a user profile. You can do this by either logging in to Facebook or by manually entering your details.")
				.setPositiveButton("Facebook", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setClass(ProfileManagerActivity.this, SettingsActivity.class);
						i.putExtra("from_profilemanager", true);
						startActivity(i);
						dialog.dismiss();
						finish();
					}
				}).setNegativeButton("Enter details", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						enterDetails();
						dialog.dismiss();
					}
				}).create();
		dialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				finish();
			}
		});
		dialog.show();
	}

	private void enterDetails() {
		View view = LayoutInflater.from(ProfileManagerActivity.this).inflate(R.layout.dialog_userprofile, null);
		final EditText first = (EditText) view.findViewById(R.id.edittext_firstname);
		final EditText last = (EditText) view.findViewById(R.id.edittext_lastname);
		final EditText birthday = (EditText) view.findViewById(R.id.edittext_birthdate);
		final GregorianCalendar birthDate = new GregorianCalendar();

		birthday.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {

				if (hasFocus) {
					AlertDialog.Builder builder = new AlertDialog.Builder(ProfileManagerActivity.this);
					AlertDialog dialog;

					View view = LayoutInflater.from(ProfileManagerActivity.this).inflate(R.layout.dialog_datepicker, null);

					final DatePicker picker = (DatePicker) view.findViewById(R.id.datepicker);

					picker.setMaxDate(birthDate.getTimeInMillis());
					picker.setMaxDate(new GregorianCalendar().getTimeInMillis());

					dialog = builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							birthDate.set(GregorianCalendar.DAY_OF_MONTH, picker.getDayOfMonth());
							birthDate.set(GregorianCalendar.MONTH, picker.getMonth());
							birthDate.set(GregorianCalendar.YEAR, picker.getYear());
							String dateString = birthDate.get(GregorianCalendar.DAY_OF_MONTH) + " "
									+ birthDate.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG, Locale.US) + " "
									+ birthDate.get(GregorianCalendar.YEAR);
							birthday.setText(dateString);
							dialog.dismiss();
						}
					}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).setTitle("Date of birth").create();
					dialog.show();
				}
			}
		});
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ProfileManagerActivity.this);
		final AlertDialog detailDialog;
		detailDialog = dialogBuilder.setTitle("User profile").setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
//				if (infoIsValid()) { TODO
					db.open();
					db.createEntry(first.getText().toString(), last.getText().toString(), birthDate, null, null, null);
					db.createEntry(first.getText().toString(), last.getText().toString(), birthDate, null, null, null);
					new ProfilePopulator().execute(db.fetchAllEntries());

//				}

			}

			private boolean infoIsValid() {
				boolean valid = true;
				if (first.getText().length() < 1) {
					valid = false;
				}
				if (last.getText().length() < 1) {
					valid = false;
				}
				if (birthday.getText().length() < 1) {
					valid = false;
				}
				return valid;
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				createUserProfile();
			}
		}).create();
		detailDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface dialog) {
				detailDialog.dismiss();
				createUserProfile();
			}
		});
		detailDialog.show();
	}

	private class ProfileAdapter extends ArrayAdapter<Profile> {

		public ProfileAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}

		public View getView(int p, View view, ViewGroup parent) {

			if (view == null) {
				view = LayoutInflater.from(ProfileManagerActivity.this).inflate(R.layout.list_item_profile, null);
			}

			ImageView picture = (ImageView) view.findViewById(R.id.imageview_profile_pic);
			TextView name = (TextView) view.findViewById(R.id.textview_profile_name);
			if (getItem(p).getPicture() != null) {
				picture.setImageBitmap(getItem(p).getPicture());
			} else {
				picture.setImageBitmap(BitmapFactory.decodeResource(ProfileManagerActivity.this.getResources(), android.R.drawable.ic_menu_report_image));
			}
			name.setText(getItem(p).getFirstName() + " " + getItem(p).getLastName());

			return view;

		}

	}
}
