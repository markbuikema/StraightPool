package com.markbuikema.straightpool;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

public class CreateProfileActivity extends Activity {

	public static final int FACEBOOK = 0;
	public static final int TWITTER = 1;

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
	private static final int PICK_CONTACT = 69;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_profile);

		pdb = ProfileDatabase.getInstance(CreateProfileActivity.this);
		pdb.open();
		entryCount = pdb.getCount();
		pdb.close();

		cancelButton = (Button) findViewById(R.id.button_cancel);
		createButton = (Button) findViewById(R.id.button_create);
		birthDateView = (EditText) findViewById(R.id.edittext_birthdate);
		firstNameView = (EditText) findViewById(R.id.edittext_firstname);
		lastNameView = (EditText) findViewById(R.id.edittext_lastname);

		birthDateView.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					AlertDialog.Builder builder = new AlertDialog.Builder(CreateProfileActivity.this);
					AlertDialog dialog;
					final DatePicker picker = new DatePicker(CreateProfileActivity.this);
					if (birthDate == null) {
						birthDate = new GregorianCalendar();
						picker.setMaxDate(new GregorianCalendar(1990, 0, 1).getTimeInMillis());
					} else {
						picker.setMaxDate(birthDate.getTimeInMillis());
					}
					picker.setMaxDate(new GregorianCalendar().getTimeInMillis());

					dialog = builder.setView(picker).setPositiveButton("OK", new DialogInterface.OnClickListener() {

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
				return false;

			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});

		createButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				new SaveProfile().execute();

			}
		});

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.activity_create_profile, menu);
		menu.findItem(R.id.menu_import_contacts).setIcon(getIconFromPackageName("com.android.contacts"));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_import_contacts:
			readContact();
			break;
		}
		return false;
	}

	public void readContact() {
		try {
			Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, PhoneLookup.CONTENT_FILTER_URI);

			contactPickerIntent.setType("vnd.android.cursor.dir/phone");
			startActivityForResult(contactPickerIntent, PICK_CONTACT);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
		case (PICK_CONTACT):
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
							while (birthdayCur.moveToNext()) {
								int bdayIndex = birthdayCur.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE);
								if (bdayIndex == -1) {
									birthDate = null;
									birthDateView.setText("");
								} else {
								String birthday = birthdayCur.getString(bdayIndex);

								if (birthDate == null) {
									birthDate = new GregorianCalendar();
								}

								birthDate.set(GregorianCalendar.DAY_OF_MONTH, Integer.valueOf(birthday.split("-")[2]));
								birthDate.set(GregorianCalendar.MONTH, Integer.valueOf(birthday.split("-")[1]) - 1);
								birthDate.set(GregorianCalendar.YEAR, Integer.valueOf(birthday.split("-")[0]));
								String dateString = birthDate.get(GregorianCalendar.DAY_OF_MONTH) + " "
										+ birthDate.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG, Locale.US) + " "
										+ birthDate.get(GregorianCalendar.YEAR);
								birthDateView.setText(dateString);
								}
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
