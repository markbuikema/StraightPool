package com.markbuikema.straightpool;

import java.util.GregorianCalendar;
import java.util.Locale;

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
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.content.CursorLoader;
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

	@SuppressWarnings("deprecation")
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
		case (PICK_CONTACT):
			if (resultCode == Activity.RESULT_OK) {
				Uri contactData = data.getData();
				String[] projection = new String[] {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID};
				Cursor c = managedQuery(contactData, projection, null, null, null);
				startManagingCursor(c);
				if (c.moveToFirst()) {

					String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
	        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					firstNameView.setText(name);

          Toast.makeText(getApplicationContext(),name , Toast.LENGTH_SHORT).show();

	        
	        ContentResolver bd = getContentResolver();
	        String where = Data.RAW_CONTACT_ID+" = "+id+" and "+Data.MIMETYPE+" = "+CommonDataKinds.Event.CONTENT_ITEM_TYPE;
	        Cursor bdc = bd.query(ContactsContract.Data.CONTENT_URI, new String[] {CommonDataKinds.Event.START_DATE}, where, null, null);
	        if (bdc.getCount() > 0) {
	            while (bdc.moveToNext()) {
	                String birthday = bdc.getString(0);
	                Toast.makeText(getApplicationContext(), id+name+birthday, Toast.LENGTH_SHORT);
	            }
	        }
					Toast.makeText(this, name, Toast.LENGTH_LONG).show();
				}
			}
			break;
		}
		
		

	}
	
	

	// method to get name, contact id, and birthday
	@SuppressWarnings("deprecation")
	private Cursor getContactsBirthdays() {
		Uri uri = ContactsContract.Data.CONTENT_URI;

		String[] projection = new String[] { ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Event.CONTACT_ID,
				ContactsContract.CommonDataKinds.Event.START_DATE };

		String where = ContactsContract.Data.MIMETYPE + "= ? AND " + ContactsContract.CommonDataKinds.Event.TYPE + "="
				+ ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY;
		String[] selectionArgs = new String[] { ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE };
		String sortOrder = null;
		CursorLoader cl = new CursorLoader(CreateProfileActivity.this);
		cl.setProjection(projection);
		cl.setSelection(where);
		cl.setSelectionArgs(selectionArgs);
		cl.setSortOrder(sortOrder);
		cl.setUri(uri);

		return cl.loadInBackground();
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
