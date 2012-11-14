package com.markbuikema.straightpool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;

public class MainActivity extends Activity {

	private GridView playerPickerList;
	private ListView savedGamesList;
	private Button startGameButton;

	public static final String KEY_FIRSTNAME = "firstname";
	public static final String KEY_LASTNAME = "lastname";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_BIRTHDATE = "birthdate";
	public static final String KEY_FACEBOOK_ID = "fbid";
	public static final String KEY_TWITTER_ID = "twitterid";

	public static final int COLUMN_COUNT = 4;

	private ProfileDatabase pdb;
	private ProfileAdapter profileAdapter;

	private Facebook facebook;

	private int cellWidth;

	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_menu);
		super.onCreate(savedInstanceState);
		pdb = ProfileDatabase.getInstance(this);

		facebook = FacebookInstance.get();
		setScreenDimensions();

		playerPickerList = (GridView) findViewById(R.id.gridview_playerpicker);
		playerPickerList.setNumColumns(COLUMN_COUNT);
		savedGamesList = (ListView) findViewById(R.id.listview_savedgames);
		startGameButton = (Button) findViewById(R.id.button_start);

		profileAdapter = new ProfileAdapter(this, 0);

		playerPickerList.setAdapter(profileAdapter);
		playerPickerList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				profileAdapter.toggle(arg2);
			}

		});

		startGameButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				ArrayList<String> selected = new ArrayList<String>();
				for (int i = 0; i < profileAdapter.getCount(); i++) {
					Profile p = profileAdapter.getItem(i);
					if (profileAdapter.isSelected(i)) {
						selected.add(p.getId());
					}
				}

				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setClass(MainActivity.this, GameActivity.class);

				i.putExtra("profile_ids", selected);
				startActivity(i);
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		FacebookInstance.get().authorizeCallback(requestCode, resultCode, data);

		invalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.activity_profile_manager, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		if (facebook.isSessionValid()) {
			menu.findItem(R.id.menu_facebook).setIcon(null).setTitle("Logging in...");
			new PictureRetriever("me") {
				@Override
				public void onPostExecute(HashMap<String, Object> bmp) {
					menu.findItem(R.id.menu_facebook).setIcon(new BitmapDrawable(getResources(), getBitmapWithFacebookOverlay((Bitmap) bmp.get("picture"))))
							.setTitle("Logged in as " + bmp.get("name"));
				}
			}.execute();
		} else {
			menu.findItem(R.id.menu_facebook)
					.setIcon(new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.ic_facebook)))
					.setTitle("Login to Facebook");
		}
		return true;
	}

	protected Bitmap getBitmapWithFacebookOverlay(Bitmap bitmap) {

		Bitmap map = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
		int[] allpixels = new int[bitmap.getHeight() * bitmap.getWidth()];

		bitmap.getPixels(allpixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		map.setPixels(allpixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		Canvas c = new Canvas(map);
		Rect rect = c.getClipBounds();
		rect.right = rect.right / 2;
		rect.top = rect.bottom / 2;
		Paint paint = new Paint();
		c.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_facebook), null, rect, paint);
		return map;
	}

	private class Logout extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				facebook.logout(MainActivity.this);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		public void onPostExecute(Void v) {
			invalidateOptionsMenu();
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_profile:
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setClass(this, CreateProfileActivity.class);
			i.putExtra("userprofile", false);
			startActivity(i);
			break;
		case R.id.menu_facebook:
			if (facebook.isSessionValid()) {

				new Logout().execute();

			} else {

				facebookLogin();

			}
		}
		return false;
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

			}

			public void onFacebookError(FacebookError error) {
			}

			public void onError(DialogError e) {
			}

			public void onCancel() {
			}
		});
	}

	@SuppressLint("NewApi")
	private void setScreenDimensions() {
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point p = new Point();
		display.getSize(p);

		cellWidth = (int) ((Math.min(p.x, p.y) - 2 * getResources().getDimension(R.dimen.default_margin)) / COLUMN_COUNT);
	}

	private class ProfileAdapter extends ArrayAdapter<Profile> {

		private ArrayList<Boolean> selected;

		public ProfileAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			selected = new ArrayList<Boolean>();

		}

		@Override
		public void add(Profile p) {
			selected.add(getCount() == 0);
			super.add(p);
		}

		public boolean isSelected(int index) {
			return selected.get(index);
		}

		public int getSelectedCount() {
			int count = 0;
			for (boolean s : selected) {
				if (s) {
					count++;
				}
			}
			return count;
		}

		public void toggle(int index) {
			selected.set(index, !selected.get(index));

			if (getSelectedCount() == 0) {
				selected.set(index, true);
			}

			notifyDataSetChanged();
		}

		@SuppressLint("NewApi")
		public View getView(int p, View view, ViewGroup parent) {

			if (view == null) {
				view = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item_profile, null);
			}

			view.setLayoutParams(new GridView.LayoutParams(cellWidth, cellWidth));

			final ImageView picture = (ImageView) view.findViewById(R.id.imageview_profile_pic);
			TextView name = (TextView) view.findViewById(R.id.textview_profile_name);
			LinearLayout border = (LinearLayout) view.findViewById(R.id.linearlayout_border);
			final ProgressBar loader = (ProgressBar) view.findViewById(R.id.progressBar1);

			new PictureRetriever(getItem(p).getFacebookId()) {
				@Override
				protected void onPostExecute(HashMap<String, Object> bmp) {
					picture.setImageBitmap((Bitmap) bmp.get("picture"));
					loader.setVisibility(View.INVISIBLE);
				}
			}.execute();
			name.setText(getItem(p).getFirstName() + " " + getItem(p).getLastName());

			if (isSelected(p)) {
				border.setVisibility(View.VISIBLE);
				picture.setImageAlpha(255);
			} else {
				border.setVisibility(View.INVISIBLE);
				picture.setImageAlpha(100);
			}

			return view;

		}

	}

	@Override
	public void onResume() {
		new PopulateProfiles().execute();
		super.onResume();
	}

	private class PopulateProfiles extends AsyncTask<Void, Profile, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			pdb.open();
			Cursor profiles = pdb.fetchAllEntries();
			profiles.moveToFirst();
			while (profiles.moveToNext()) {
				GregorianCalendar birthday = new GregorianCalendar();
				String bday = profiles.getString(profiles.getColumnIndex(KEY_BIRTHDATE));
				int day = Integer.valueOf(bday.split("-")[0]);
				int month = Integer.valueOf(bday.split("-")[1]);
				int year = Integer.valueOf(bday.split("-")[2]);
				birthday.set(year, month, day);

				publishProgress(new Profile(profiles.getString(profiles.getColumnIndex(KEY_ROWID)),
						profiles.getString(profiles.getColumnIndex(KEY_FIRSTNAME)), profiles.getString(profiles.getColumnIndex(KEY_LASTNAME)), birthday,
						profiles.getString(profiles.getColumnIndex(KEY_FACEBOOK_ID))));
			}
			profiles.close();
			return null;
		}

		@Override
		public void onPreExecute() {
			profileAdapter.clear();
		}

		@Override
		protected void onProgressUpdate(Profile... p) {
			for (Profile profile : p) {
				profileAdapter.add(profile);
			}
		}

		@Override
		public void onPostExecute(Void v) {
			pdb.close();
			if (profileAdapter.getCount() == 0) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setClass(MainActivity.this, InitializationActivity.class);
				startActivity(i);
				finish();
			}
		}

	}

}
