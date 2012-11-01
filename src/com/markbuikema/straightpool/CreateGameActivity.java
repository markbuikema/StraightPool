package com.markbuikema.straightpool;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CreateGameActivity extends Activity {

	public static final String KEY_FIRSTNAME = "firstname";
	public static final String KEY_LASTNAME = "lastname";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_BIRTHDATE = "birthdate";
	public static final String KEY_PICTURE_URL = "pic_url";
	public static final String KEY_FACEBOOK_ID = "fbid";
	public static final String KEY_TWITTER_ID = "twitterid";

	private ProfileDatabase pdb;
	private ProfileAdapter adapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView list = new ListView(this);
		setContentView(list);
		pdb = ProfileDatabase.getInstance(this);
		pdb.open();

		adapter = new ProfileAdapter(this, 0);

		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				adapter.getItem(arg2).setSelected(!adapter.getItem(arg2).isSelected());
			}

		});

		new Populate().execute(pdb.fetchAllEntries());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.activity_create_game, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_play:
			ArrayList<String> game = new ArrayList<String>();

			for (int i = 0; i < adapter.getCount(); i++) {
				if (adapter.getItem(i).isSelected()) {
					Profile p = adapter.getItem(i);
					game.add(p.getFirstName());
					game.add(p.getLastName());
					GregorianCalendar birthDate = p.getBirthday();
					game.add(birthDate.get(GregorianCalendar.DAY_OF_MONTH) + "-" + birthDate.get(GregorianCalendar.MONTH) + "-"
							+ birthDate.get(GregorianCalendar.YEAR));
					game.add(p.getPictureUrl());
					game.add(p.getFacebookId());
					game.add(p.getTwitterId());
				}
			}

			if (game.size() > 0) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setClass(this, MainActivity.class);
				i.putExtra("game", game);
				startActivity(i);
			} else {
				Toast.makeText(CreateGameActivity.this, "Please select at least 1 profile.", Toast.LENGTH_SHORT).show();
			}
			break;
		}
		return false;
	}

	private class ListItem extends Profile {

		private boolean selected;

		public ListItem(String firstName, String lastName, GregorianCalendar birthday, String pictureUrl, Bitmap picture, String facebookId,
				String twitterId) {
			super("",firstName, lastName, birthday, pictureUrl, picture, facebookId, twitterId);
			selected = false;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
			adapter.notifyDataSetChanged();
		}
	}

	private class Populate extends AsyncTask<Cursor, ListItem, Void> {

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

				publishProgress(new ListItem(profiles.getString(profiles.getColumnIndex(KEY_FIRSTNAME)), profiles.getString(profiles
						.getColumnIndex(KEY_LASTNAME)), birthday, profiles.getString(profiles.getColumnIndex(KEY_PICTURE_URL)), picture,
						profiles.getString(profiles.getColumnIndex(KEY_FACEBOOK_ID)), profiles.getString(profiles.getColumnIndex(KEY_TWITTER_ID))));
			}
			profiles.close();
			return null;
		}

		@Override
		public void onPreExecute() {
			adapter.clear();
		}

		@Override
		protected void onProgressUpdate(ListItem... p) {
			for (Profile profile : p) {
				adapter.add((ListItem) profile);
			}
		}

		@Override
		public void onPostExecute(Void v) {
			pdb.close();
		}

	}

	private class ProfileAdapter extends ArrayAdapter<ListItem> {

		public ProfileAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}

		public View getView(int p, View view, ViewGroup parent) {

			if (view == null) {
				view = LayoutInflater.from(CreateGameActivity.this).inflate(R.layout.list_item_profile_picker, null);
			}

			ImageView picture = (ImageView) view.findViewById(R.id.imageview_profile_pic);
			TextView name = (TextView) view.findViewById(R.id.textview_profile_name);
			if (getItem(p).isSelected()) {
				name.setTextColor(getResources().getColor(R.color.blue));
			} else {
				name.setTextColor(Color.BLACK);
			}
			if (getItem(p).getPicture() != null) {
				picture.setImageBitmap(getItem(p).getPicture());
			} else {
				picture.setImageBitmap(BitmapFactory.decodeResource(CreateGameActivity.this.getResources(), android.R.drawable.ic_menu_report_image));
			}
			name.setText(getItem(p).getFirstName() + " " + getItem(p).getLastName());

			return view;

		}

	}

}
