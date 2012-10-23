package com.markbuikema.straightpool;

import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

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

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_manager);
		db = ProfileDatabase.getInstance(this);

		profileList = (GridView) findViewById(R.id.gridview_profiles);
		adapter = new ProfileAdapter(this, 0);

		profileList.setAdapter(adapter);
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

				Bitmap picture = TwitterAuthActivity.getBitmap(profiles.getString(profiles.getColumnIndex(KEY_PICTURE_URL)));

				publishProgress(new Profile(profiles.getString(profiles.getColumnIndex(KEY_FIRSTNAME)), profiles.getString(profiles
						.getColumnIndex(KEY_LASTNAME)), birthday, profiles.getString(profiles.getColumnIndex(KEY_PICTURE_URL)), picture, profiles.getString(profiles.getColumnIndex(KEY_FACEBOOK_ID)),
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
		}

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
