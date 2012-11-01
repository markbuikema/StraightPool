package com.markbuikema.straightpool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.Facebook;

public class CreateProfileActivity extends Activity {

	public static final int FACEBOOK = 0;
	public static final int TWITTER = 1;
	public static final int CONTACT = 2;

	private String profileId;

	private ProfileDatabase pdb;
	private int entryCount;

	private GregorianCalendar birthDate;
	private String pictureUrl;
	private String facebookId;
	private String twitterId;

	private EditText birthDateView;
	private EditText firstNameView;
	private EditText lastNameView;
	private Button cancelButton;
	private Button createButton;

	private Facebook facebook;

	private Handler mHandler = new Handler();
	private Button facebookLink;
	private Button twitterLink;
	
	

	private AuthorizationManager am = AuthorizationManager.getInstance();
	private ImageView facebookIcon;
	private ImageView twitterIcon;
	private ProgressBar facebookLoader;
	private ProgressBar twitterLoader;
	private ImageView pictureView;
	private Button facebookCancel;
	private Button twitterCancel;

	private Bitmap facebookImage;
	private Bitmap twitterImage;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_profile);

		profileId = getIntent().getStringExtra("profileId");
		
		

		pictureUrl = getIntent().getStringExtra("url");
		twitterId = getIntent().getStringExtra("twitterId");
		facebookId = getIntent().getStringExtra("facebookId");

		facebookImage = BitmapFactory.decodeResource(getResources(), R.drawable.ic_facebook);
		twitterImage = BitmapFactory.decodeResource(getResources(), R.drawable.ic_twitter);

		facebook = FacebookInstance.get(this);
		am.setContext(this);

		pdb = ProfileDatabase.getInstance(CreateProfileActivity.this);
		pdb.open();
		entryCount = pdb.getCount();
		pdb.close();

		cancelButton = (Button) findViewById(R.id.button_cancel);
		createButton = (Button) findViewById(R.id.button_create);
		birthDateView = (EditText) findViewById(R.id.edittext_birthdate);
		firstNameView = (EditText) findViewById(R.id.edittext_firstname);
		lastNameView = (EditText) findViewById(R.id.edittext_lastname);

		facebookLink = (Button) findViewById(R.id.button_facebooklink);
		twitterLink = (Button) findViewById(R.id.button_twitterlink);

		facebookIcon = (ImageView) findViewById(R.id.imageview_facebook);
		twitterIcon = (ImageView) findViewById(R.id.imageview_twitter);

		facebookLoader = (ProgressBar) findViewById(R.id.progressbar_facebook);
		twitterLoader = (ProgressBar) findViewById(R.id.progressbar_twitter);

		facebookCancel = (Button) findViewById(R.id.button_removefacebook);
		twitterCancel = (Button) findViewById(R.id.button_removetwitter);

		pictureView = (ImageView) findViewById(R.id.imageview_profilepic);

		firstNameView.setText(getIntent().getStringExtra("first") == null ? "" : getIntent().getStringExtra("first"));
		lastNameView.setText(getIntent().getStringExtra("last") == null ? "" : getIntent().getStringExtra("last"));

		onBirthdayChanged((GregorianCalendar) getIntent().getSerializableExtra("bday"));

		if (pictureUrl != null) {
			new PutPicture().execute();
		}

		if (twitterId != null) {
			onTwitterLinkChanged(twitterId);
		}

		if (facebookId != null) {
			onFacebookLinkChanged(facebookId);
		}

		if (profileId != null) {
			createButton.setText("Edit");
		}

		facebookLink.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				new FacebookFriendsPicker().execute(false);
				facebookLink.setEnabled(false);
			}
		});

		twitterLink.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				new LinkTwitter().execute();
				twitterLink.setEnabled(false);
			}
		});

		facebookLink.setOnLongClickListener(new OnLongClickListener() {

			public boolean onLongClick(View v) {
				if (facebookId != null) {
					facebookCancel.setVisibility(View.VISIBLE);
				}
				return false;
			}
		});

		twitterLink.setOnLongClickListener(new OnLongClickListener() {

			public boolean onLongClick(View v) {
				if (twitterId != null) {
					twitterCancel.setVisibility(View.VISIBLE);
				}
				return false;
			}
		});

		facebookCancel.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				onFacebookLinkChanged(null);
				facebookCancel.setVisibility(View.GONE);
			}
		});

		twitterCancel.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				onTwitterLinkChanged(null);
				twitterCancel.setVisibility(View.GONE);
			}
		});

		birthDateView.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {

				if (hasFocus) {
					AlertDialog.Builder builder = new AlertDialog.Builder(CreateProfileActivity.this);
					AlertDialog dialog;

					View view = LayoutInflater.from(CreateProfileActivity.this).inflate(R.layout.dialog_datepicker, null);

					final DatePicker picker = (DatePicker) view.findViewById(R.id.datepicker);

					if (birthDate == null) {
						birthDate = new GregorianCalendar();
						picker.setMaxDate(new GregorianCalendar(1990, 0, 1).getTimeInMillis());
					} else {
						picker.setMaxDate(birthDate.getTimeInMillis());
					}
					picker.setMaxDate(new GregorianCalendar().getTimeInMillis());

					dialog = builder.setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							birthDate.set(GregorianCalendar.DAY_OF_MONTH, picker.getDayOfMonth());
							birthDate.set(GregorianCalendar.MONTH, picker.getMonth());
							birthDate.set(GregorianCalendar.YEAR, picker.getYear());
							String dateString = birthDate.get(GregorianCalendar.DAY_OF_MONTH) + " "
									+ birthDate.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG, Locale.US) + " "
									+ birthDate.get(GregorianCalendar.YEAR);
							birthDateView.setText(dateString);
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

		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});

		createButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (profileId == null) {
					if (infoIsValid()) {
						new SaveProfile().execute();
					} else {
						AlertDialog.Builder builder = new AlertDialog.Builder(CreateProfileActivity.this);
						AlertDialog dialog;
						dialog = builder.setTitle("Could not create profile").setMessage("Please fill in all information.")
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								}).create();
						dialog.show();
					}
				} else {
					if (infoIsValid()) {
						new UpdateProfile().execute();
					} else {
						AlertDialog.Builder builder = new AlertDialog.Builder(CreateProfileActivity.this);
						AlertDialog dialog;
						dialog = builder.setTitle("Could not update profile").setMessage("Please fill in all information.")
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								}).create();
						dialog.show();
					}
				}

			}
		});

	}
	
	private boolean infoIsValid() {
		boolean valid = true;
		if (firstNameView.getText().length() < 1) {
			valid = false;
		}
		if (lastNameView.getText().length() < 1) {
			valid = false;
		}
		if (birthDateView.getText().length() < 1) {
			valid = false;
		}
		return valid;
	}

	private class SaveProfile extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			pdb.open();
			pdb.createEntry(firstNameView.getText().toString(), lastNameView.getText().toString(), birthDate, pictureUrl, facebookId, twitterId);

			while (entryCount == pdb.getCount()) {
			}

			pdb.close();

			return null;
		}

		@Override
		public void onPostExecute(Void v) {
			finish();
		}

	}

	private class UpdateProfile extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			pdb.open();
			pdb.replace(profileId, firstNameView.getText().toString(), lastNameView.getText().toString(), birthDate, pictureUrl, facebookId, twitterId);

			pdb.close();

			return null;
		}

		@Override
		public void onPostExecute(Void v) {
			finish();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.activity_create_profile, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_import:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final AlertDialog dialog;
			ListView list = new ListView(this);
			ArrayAdapter<String> importAdapter = new ArrayAdapter<String>(this, 0) {
				@Override
				public View getView(int p, View v, ViewGroup g) {
					if (v == null) {
						v = LayoutInflater.from(CreateProfileActivity.this).inflate(R.layout.list_item_import, null);
					}
					ImageView icon = (ImageView) v.findViewById(R.id.icon);
					TextView medium = (TextView) v.findViewById(R.id.textview_medium);

					medium.setText(getItem(p));

					switch (p) {
					case 0:
						icon.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_facebook));
						break;
					case 1:
						icon.setImageDrawable(getIconFromPackageName("com.android.contacts"));
					}

					return v;
				}
			};
			list.setAdapter(importAdapter);
			importAdapter.add("Import from Facebook");
			importAdapter.add("Import from contacts");
			dialog = builder.setTitle("Import profile").setView(list).setCancelable(true).create();
			dialog.show();
			list.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					switch (arg2) {
					case 0:
						readFacebook();
						break;
					case 1:
						readContacts();
						break;
					}
					dialog.dismiss();
				}

			});
			break;
		}
		return false;
	}

	protected void readFacebook() {

		new FacebookFriendsPicker().execute(true);

	}

	private class FacebookFriendsListItem {
		private String name;
		private String first;
		private String last;
		private String id;
		private GregorianCalendar bday;

		public FacebookFriendsListItem(String name, String first, String last, String id, GregorianCalendar bday) {
			this.name = name;
			this.first = first;
			this.last = last;
			this.id = id;
			this.bday = bday;
		}

		public String getName() {
			return name;
		}

		public String getFirst() {
			return first;
		}

		public String getLast() {
			return last;
		}

		public String getId() {
			return id;
		}

		public GregorianCalendar getBday() {
			return bday;
		}

	}

	private class FacebookFriendsPicker extends AsyncTask<Boolean, Void, Void> {

		AlertDialog loader;

		@Override
		protected Void doInBackground(Boolean... arg0) {

			publishProgress();
			final boolean importing = arg0[0];
			Bundle params = new Bundle();
			params.putString("fields", "birthday,first_name,last_name,name");
			String response;
			try {
				response = facebook.request("me/friends", params);

				final ArrayAdapter<FacebookFriendsListItem> friends = new ArrayAdapter<FacebookFriendsListItem>(CreateProfileActivity.this, 0) {
					@Override
					public View getView(int p, View v, ViewGroup vg) {

						if (v == null) {
							v = LayoutInflater.from(CreateProfileActivity.this).inflate(R.layout.list_item_import, null);
						}

						ImageView icon = (ImageView) v.findViewById(R.id.icon);
						icon.setVisibility(View.GONE);

						TextView name = (TextView) v.findViewById(R.id.textview_medium);
						name.setText(getItem(p).getName());

						return v;
					}
				};

				try {
					JSONObject object = new JSONObject(response);

					JSONArray array = object.getJSONArray("data");
					for (int i = 0; i < array.length(); i++) {

						JSONObject friend = array.getJSONObject(i);

						String first = friend.getString("first_name");
						String last = friend.getString("last_name");
						String name = friend.getString("name");
						String id = friend.getString("id");
						String bdayString;
						try {
							bdayString = friend.getString("birthday");
						} catch (JSONException jsone) {
							bdayString = null;
						}

						GregorianCalendar birthday = null;
						if (bdayString != null) {
							String[] dateArray = bdayString.split("/");
							if (dateArray.length == 3) {
								birthday = new GregorianCalendar();
								birthday.set(Integer.parseInt(dateArray[2]), Integer.parseInt(dateArray[0]) - 1, Integer.parseInt(dateArray[1]));
							} else {
								birthday = null;
							}
						}

						friends.add(new FacebookFriendsListItem(name, first, last, id, birthday));

					}

					mHandler.post(new Runnable() {

						public void run() {

							AlertDialog.Builder builder = new AlertDialog.Builder(CreateProfileActivity.this);

							ListView friendList = new ListView(CreateProfileActivity.this);
							final AlertDialog dialog = builder.setTitle(importing ? "Import Facebook friend" : "Link to Facebook").setView(friendList).create();

							dialog.setOnDismissListener(new OnDismissListener() {

								public void onDismiss(DialogInterface dialog) {
									facebookLink.setEnabled(true);
								}
							});
							friendList.setOnItemClickListener(new OnItemClickListener() {

								public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

									FacebookFriendsListItem item = friends.getItem(arg2);

									onFacebookLinkChanged(item.getId());

									if (importing) {

										onBirthdayChanged(item.getBday());
										firstNameView.setText(item.getFirst());
										lastNameView.setText(item.getLast());
									}

									facebookLink.setEnabled(true);
									dialog.dismiss();
								}
							});
							friendList.setAdapter(friends);
							dialog.show();
						}
					});

				} catch (JSONException e) {
					e.printStackTrace();
				}
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return null;
		}

		@Override
		public void onProgressUpdate(Void... v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(CreateProfileActivity.this);
			View view = LayoutInflater.from(CreateProfileActivity.this).inflate(R.layout.dialog_loading, null);
			loader = builder.setTitle("Loading Facebook contacts...").setView(view).create();
			loader.setOnDismissListener(new OnDismissListener() {

				public void onDismiss(DialogInterface dialog) {
					facebookLink.setEnabled(true);
				}
			});
			loader.show();

		}

		@Override
		public void onPostExecute(Void v) {
			loader.dismiss();
		}
	}

	public void toast(String text) {
		Toast.makeText(CreateProfileActivity.this, text, Toast.LENGTH_SHORT).show();
	}

	public void onBirthdayChanged(GregorianCalendar date) {
		if (date == null) {
			birthDate = null;
			birthDateView.setText("");
		} else {
			birthDate = date;
			String birthdayString = birthDate.get(GregorianCalendar.DAY_OF_MONTH) + " "
					+ birthDate.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG, Locale.US) + " " + birthDate.get(GregorianCalendar.YEAR);
			birthDateView.setText(birthdayString);
		}
	}

	public void onFacebookLinkChanged(String id) {
		this.facebookId = id;
		if (facebookId != null) {
			new PutLinkData(facebookId, FACEBOOK).execute();
		} else {
			facebookLink.setText("Not linked to Facebook");
			facebookIcon.setImageBitmap(facebookImage);
		}
		facebookCancel.setVisibility(View.GONE);
	}

	public void onTwitterLinkChanged(String id) {
		this.twitterId = id;
		if (twitterId != null) {
			new PutLinkData(twitterId, TWITTER).execute();
		} else {
			twitterLink.setText("Not linked to Twitter");
			twitterIcon.setImageBitmap(twitterImage);
		}
		twitterCancel.setVisibility(View.GONE);
	}

	public class PutLinkData extends AsyncTask<Void, Void, Bitmap> {

		private String facebookId;
		private String name;
		private int mediaType;

		public PutLinkData(String facebookId, int mediaType) {
			this.mediaType = mediaType;
			this.facebookId = facebookId;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {

			Bitmap bitmap = null;
			try {
				switch (mediaType) {
				case FACEBOOK:
					mHandler.post(new Runnable() {
						public void run() {
							facebookLoader.setVisibility(View.VISIBLE);
						}
					});
					Bundle fields = new Bundle();
					fields.putString("fields", "name");
					JSONObject object = new JSONObject(facebook.request(facebookId, fields));
					name = object.getString("name");
					URL facebookUrl = new URL("https://graph.facebook.com/" + facebookId + "/picture?type=square&method=GET&access_token="
							+ facebook.getAccessToken());
					pictureUrl = facebookUrl.toExternalForm();
					bitmap = BitmapFactory.decodeStream(facebookUrl.openConnection().getInputStream());
					break;
				case TWITTER:
					mHandler.post(new Runnable() {
						public void run() {
							twitterLoader.setVisibility(View.VISIBLE);
						}
					});
					JSONArray users = new JSONArray(TwitterAuthActivity.getContent("https://api.twitter.com/1/users/lookup.json?user_id=" + twitterId));
					name = users.getJSONObject(0).getString("name");
					URL twitterUrl = new URL("https://api.twitter.com/1/users/profile_image/" + twitterId + ".json?size=normal");
					pictureUrl = twitterUrl.toExternalForm();
					bitmap = BitmapFactory.decodeStream(twitterUrl.openConnection().getInputStream());
					break;

				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return bitmap;
		}

		public void onPostExecute(Bitmap bmp) {
			if (mediaType == FACEBOOK) {
				facebookIcon.setImageBitmap(getRoundedCornerBitmap(bmp, 4));
				facebookLink.setText("Linked to " + name + "'s Facebook account");
				facebookLoader.setVisibility(View.GONE);
			} else {
				twitterIcon.setImageBitmap(getRoundedCornerBitmap(bmp, 4));
				twitterLink.setText("Linked to " + name + "'s Twitter account");
				twitterLoader.setVisibility(View.GONE);
			}

			pictureView.setImageBitmap(getRoundedCornerBitmap(bmp, 4));
		}

	}

	public Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = Color.WHITE;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = pixels;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	private class TwitterListItem {
		private String name;
		private String id;

		public TwitterListItem(String name, String id) {
			this.name = name;
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

	}

	private class LinkTwitter extends AsyncTask<Void, Void, Void> {

		private ArrayAdapter<TwitterListItem> adapter;
		private AlertDialog loader;

		public LinkTwitter() {

			adapter = new ArrayAdapter<TwitterListItem>(CreateProfileActivity.this, 0) {
				public View getView(int p, View v, ViewGroup vg) {
					if (v == null) {
						v = LayoutInflater.from(CreateProfileActivity.this).inflate(R.layout.list_item_import, null);
					}

					ImageView icon = (ImageView) v.findViewById(R.id.icon);
					icon.setVisibility(View.GONE);

					TextView name = (TextView) v.findViewById(R.id.textview_medium);
					name.setText(getItem(p).getName());

					return v;
				}
			};
		}

		@Override
		protected Void doInBackground(Void... params) {

			publishProgress();

			try {
				JSONObject json = new JSONObject(TwitterAuthActivity.getContent("https://api.twitter.com/1/friends/ids.json"));

				JSONArray ids = json.getJSONArray("ids");

				String idString = ids.toString().substring(1, ids.toString().length() - 1);

				JSONArray users = new JSONArray(TwitterAuthActivity.getContent("https://api.twitter.com/1/users/lookup.json?user_id=" + idString));
				for (int i = 0; i < users.length(); i++) {
					adapter.add(new TwitterListItem(users.getJSONObject(i).getString("name"), users.getJSONObject(i).getString("id")));
				}
				mHandler.post(new Runnable() {

					public void run() {
						AlertDialog.Builder builder = new AlertDialog.Builder(CreateProfileActivity.this);
						ListView list = new ListView(CreateProfileActivity.this);
						final AlertDialog dialog = builder.setTitle("Link to Twitter").setView(list).create();
						list.setAdapter(adapter);
						list.setOnItemClickListener(new OnItemClickListener() {

							public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
								TwitterListItem item = adapter.getItem(arg2);
								onTwitterLinkChanged(item.getId());

								dialog.dismiss();

							}
						});
						dialog.setOnDismissListener(new OnDismissListener() {

							public void onDismiss(DialogInterface dialog) {
								twitterLink.setEnabled(true);
							}
						});
						dialog.show();
					}
				});

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		public void onProgressUpdate(Void... v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(CreateProfileActivity.this);
			View view = LayoutInflater.from(CreateProfileActivity.this).inflate(R.layout.dialog_loading, null);
			loader = builder.setTitle("Loading Twitter contacts...").setView(view).create();
			loader.setOnDismissListener(new OnDismissListener() {

				public void onDismiss(DialogInterface dialog) {
					twitterLink.setEnabled(true);
				}
			});
			loader.show();

		}

		@Override
		public void onPostExecute(Void v) {
			loader.dismiss();
		}
	}

	public void readContacts() {
		try {
			Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, PhoneLookup.CONTENT_FILTER_URI);

			contactPickerIntent.setType("vnd.android.cursor.dir/phone");
			startActivityForResult(contactPickerIntent, CONTACT);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
		case (CONTACT):
			try {
				if (resultCode == Activity.RESULT_OK) {
					String displayName = "0";
					Uri contactData = data.getData();
					Log.d("URI:", contactData.toString());
					Cursor c = managedQuery(contactData, null, null, null, null);
					if (c.moveToFirst()) {
						displayName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
						firstNameView.setText(displayName);
					}

					// part2

					ContentResolver cr = CreateProfileActivity.this.getContentResolver();
					String[] projection = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME };

					Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, ContactsContract.Contacts.DISPLAY_NAME
							+ " COLLATE LOCALIZED ASC");

					while (cur.moveToNext()) {

						String columns[] = { ContactsContract.CommonDataKinds.Event.START_DATE, ContactsContract.CommonDataKinds.Event.TYPE,
								ContactsContract.CommonDataKinds.Event.MIMETYPE, };

						String where = Event.TYPE + "=" + Event.TYPE_BIRTHDAY + " and " + Event.MIMETYPE + " = '" + Event.CONTENT_ITEM_TYPE + "' and "
								+ ContactsContract.Data.DISPLAY_NAME + " = '" + displayName + "'";

						String[] selectionArgs = null;
						String sortOrder = ContactsContract.Contacts.DISPLAY_NAME;

						Cursor birthdayCur = cr.query(ContactsContract.Data.CONTENT_URI, columns, where, selectionArgs, sortOrder);
						if (birthdayCur.getCount() > 0) {
							boolean hasBirthday = false;
							while (birthdayCur.moveToNext()) {
								int bdayIndex = birthdayCur.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE);
								if (bdayIndex == -1) {
									onBirthdayChanged(null);
								} else {
									hasBirthday = true;
									String birthday = birthdayCur.getString(bdayIndex);

									GregorianCalendar date = new GregorianCalendar();

									date.set(GregorianCalendar.DAY_OF_MONTH, Integer.valueOf(birthday.split("-")[2]));
									date.set(GregorianCalendar.MONTH, Integer.valueOf(birthday.split("-")[1]) - 1);
									date.set(GregorianCalendar.YEAR, Integer.valueOf(birthday.split("-")[0]));

									onBirthdayChanged(date);
								}
							}
							if (!hasBirthday) {
								onBirthdayChanged(null);
							}
						}
						birthdayCur.close();

					}

					cur.close();

					// part3;

					String[] proj = new String[] { ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
							ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
							ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME };

					String selectionArgs = ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME + "='" + displayName + "'";
					Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, proj, selectionArgs, null, null);

					int indexGivenName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
					int indexFamilyName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
					int indexDisplayName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME);
					int indexMiddleName = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME);

					Log.d("Cursor size", cursor.getCount() + ".");
					cursor.moveToFirst();

					// while (cursor.moveToNext()) {
					Log.d("tag", "nu in whileloopje");
					String given = "-";
					String family = "-";
					String middle = "-";
					if (indexGivenName != -1) {
						given = cursor.getString(indexGivenName);
					}
					if (indexFamilyName != -1) {
						family = cursor.getString(indexFamilyName);
					}
					if (indexMiddleName != -1) {
						middle = cursor.getString(indexMiddleName);
					}
					String display = cursor.getString(indexDisplayName);
					Log.d("names", given + "," + middle + "," + family + "," + display);

					if (middle == null) {
						firstNameView.setText(given);
						lastNameView.setText(family);
						Log.d("given name: ", given);
					} else {
						firstNameView.setText(given);
						lastNameView.setText(middle + " " + family);
					}
					// }

				}
			} catch (Exception e) {
				Toast.makeText(CreateProfileActivity.this, "Exception logged", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
			break;
		}

	}

	private class PutPicture extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			// TODO

			return null;
		}

	}

	private Drawable getIconFromPackageName(final String packageName) {
		PackageManager packageManager = getPackageManager();
		try {
			Drawable icon = packageManager.getApplicationIcon(packageName);
			return icon;
		} catch (NameNotFoundException e) {
			Toast toast = Toast.makeText(this, "error in getting icon", Toast.LENGTH_SHORT);
			toast.show();
			e.printStackTrace();
		}
		return null;
	}

}
