package com.markbuikema.straightpool;

import java.util.GregorianCalendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ProfileDatabase {

	private static ProfileDatabase instance;

	public static final String KEY_FIRSTNAME = "firstname";

	public static final String KEY_LASTNAME = "lastname";

	public static final String KEY_ROWID = "_id";

	public static final String KEY_BIRTHDATE = "birthdate";

	public static final String KEY_PICTURE_URL = "pic_url";

	public static final String KEY_FACEBOOK_ID = "fbid";

	public static final String KEY_TWITTER_ID = "twitterid";

	private static final String TAG = "NotesDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_CREATE = "create table profiles (_id integer primary key autoincrement, "
			+ "firstname text not null, lastname text not null, birthdate text not null, pic_url text, fbid text, twitterid text);";

	private static final String DATABASE_NAME = "profileDatabase";
	private static final String DATABASE_TABLE = "profiles";
	private static final int DATABASE_VERSION = 2;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS notes");
			onCreate(db);
		}
	}

	/**
	 * Instantiates a new highscore database.
	 * 
	 * @param ctx
	 *          the ctx
	 */
	private ProfileDatabase(Context ctx) {
		this.mCtx = ctx;
	}

	public static ProfileDatabase getInstance(Context ctx) {
		if (instance == null) {
			instance = new ProfileDatabase(ctx);
		}
		return instance;
	}

	public static ProfileDatabase getInstance() {
		return instance;
	}

	/**
	 * Open.
	 * 
	 * @return the highscore database
	 * @throws SQLException
	 *           the sQL exception
	 */
	public ProfileDatabase open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close.
	 */
	public void close() {
		mDbHelper.close();
	}

	public long createEntry(String firstname, String lastname, GregorianCalendar birthdate, String pictureUrl, String fbid,
			String twitterid) {
		String birthdateString = birthdate.get(GregorianCalendar.DAY_OF_MONTH) + "-" + birthdate.get(GregorianCalendar.MONTH) + "-"
				+ birthdate.get(GregorianCalendar.YEAR) + "";
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_FIRSTNAME, firstname);
		initialValues.put(KEY_LASTNAME, lastname);
		initialValues.put(KEY_BIRTHDATE, birthdateString);
		initialValues.put(KEY_PICTURE_URL, pictureUrl);
		initialValues.put(KEY_FACEBOOK_ID, fbid);
		initialValues.put(KEY_TWITTER_ID, twitterid);

		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Delete entry.
	 * 
	 * @param rowId
	 *          the row id
	 * @return true, if successful
	 */
	public boolean deleteEntry(long rowId) {

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Fetch all entries.
	 * 
	 * @return the cursor
	 */
	public Cursor fetchAllEntries() {

		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_FIRSTNAME, KEY_LASTNAME, KEY_BIRTHDATE, KEY_PICTURE_URL,
				KEY_FACEBOOK_ID, KEY_TWITTER_ID }, null, null, null, null, (KEY_ROWID + " ASC"));
	}
	
	public void replace(String profileId, String firstname, String lastname, GregorianCalendar birthdate, String pictureUrl, String fbid,
			String twitterid) {
		
		String birthdateString = birthdate.get(GregorianCalendar.DAY_OF_MONTH) + "-" + birthdate.get(GregorianCalendar.MONTH) + "-"
				+ birthdate.get(GregorianCalendar.YEAR) + "";
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ROWID, profileId);
		initialValues.put(KEY_FIRSTNAME, firstname);
		initialValues.put(KEY_LASTNAME, lastname);
		initialValues.put(KEY_BIRTHDATE, birthdateString);
		initialValues.put(KEY_PICTURE_URL, pictureUrl);
		initialValues.put(KEY_FACEBOOK_ID, fbid);
		initialValues.put(KEY_TWITTER_ID, twitterid);
		
		mDb.replace(DATABASE_TABLE, null, initialValues);

	}
	
	public int getCount() {
		return (int) DatabaseUtils.queryNumEntries(mDb, DATABASE_TABLE);
		
	}

}
