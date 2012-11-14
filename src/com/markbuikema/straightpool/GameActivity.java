package com.markbuikema.straightpool;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.markbuikema.straightpool.Round.Foul;

public class GameActivity extends Activity {

	public static final String KEY_FIRSTNAME = "firstname";

	public static final String KEY_LASTNAME = "lastname";

	public static final String KEY_ROWID = "_id";

	public static final String KEY_BIRTHDATE = "birthdate";

	public static final String KEY_FACEBOOK_ID = "fbid";

	private static final int LIST_COLUMN_WIDTH = 150;

	private NumberPicker pickerRemainingBalls;
	private ListView scoreList;
	private LinearLayout profileContainer;
	private Game game;
	private ScoreAdapter scoreAdapter;
	private int screenWidth;
	private int screenHeight;
	private int savedRemainingBalls = 1;

	private Button rerackButton;
	private Button foulButton;

	protected PowerManager.WakeLock wakeLock;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "My Tag");

		Bundle extras = getIntent().getExtras();

		ArrayList<String> ids = (ArrayList<String>) extras.getSerializable("profile_ids");
		ArrayList<Profile> profiles = new ArrayList<Profile>();
		ProfileDatabase mDb = ProfileDatabase.getInstance(this);
		mDb.open();

		Cursor c = mDb.fetchAllEntries();

		while (c.moveToNext()) {

			String first = c.getString(c.getColumnIndex(KEY_FIRSTNAME));
			String last = c.getString(c.getColumnIndex(KEY_LASTNAME));
			String id = c.getString(c.getColumnIndex(KEY_ROWID));
			String facebookId = c.getString(c.getColumnIndex(KEY_FACEBOOK_ID));
			String dateString = c.getString(c.getColumnIndex(KEY_BIRTHDATE));
			GregorianCalendar birthday = new GregorianCalendar();
			int day = Integer.valueOf(dateString.split("-")[0]);
			int month = Integer.valueOf(dateString.split("-")[1]);
			int year = Integer.valueOf(dateString.split("-")[2]);
			birthday.set(year, month, day);
			if (ids.contains(id)) {
				profiles.add(new Profile(id, first, last, birthday, facebookId));
			}
		}
		c.close();
		mDb.close();

		Profile[] array = new Profile[profiles.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = profiles.get(i);
		}

		game = new Game(array);

		setContentView(R.layout.activity_main);

		SharedPreferences prefs = getSharedPreferences("settings", 0);
		setWakeLock(prefs.getBoolean("wakelock", false));

		setScreenDimensions();
		registerProfilesCaption();
		registerPickerRemainingBalls();
		registerScoreList();
		registerRerackButton();
		registerFoulButton();
	}

	@Override
	public void onStop() {
		super.onStop();
		setWakeLock(false);
	}

	private void setWakeLock(boolean wakeLock) {
		if (wakeLock) {
			this.wakeLock.acquire();
		} else {
			if (this.wakeLock.isHeld()) {
				this.wakeLock.release();
			}
		}
	}

	@SuppressLint("NewApi")
	private void setScreenDimensions() {
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point p = new Point();
		display.getSize(p);
		screenWidth = p.x;
		screenHeight = p.y;
	}

	// TODO INITIALISATION (no todo, just bookmark)

	private void registerFoulButton() {

		foulButton = (Button) findViewById(R.id.button_foul);
		foulButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
				AlertDialog dialog;

				final View view = LayoutInflater.from(GameActivity.this).inflate(R.layout.dialog_foul, null);
				final RadioGroup profileContainer = (RadioGroup) view.findViewById(R.id.radiogroup_foul_profiles);
				final int blue = GameActivity.this.getResources().getColor(R.color.blue);
				for (Profile profile : game.getPlayers()) {
					RadioButton radio = new RadioButton(GameActivity.this);
					radio.setText(profile.getFirstName());
					profileContainer.addView(radio);
				}
				((RadioButton) profileContainer.getChildAt(0)).setChecked(true);
				((RadioButton) profileContainer.getChildAt(0)).setTextColor(blue);

				profileContainer.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					public void onCheckedChanged(RadioGroup group, int checkedId) {
						for (int i = 0; i < group.getChildCount(); i++) {
							((RadioButton) group.getChildAt(i)).setTextColor(Color.BLACK);
						}
						((RadioButton) view.findViewById(checkedId)).setTextColor(blue);
					}
				});
				final RadioGroup foulTypeSelector = (RadioGroup) view.findViewById(R.id.radiogroup_foultypes);
				final EditText customFoulType = (EditText) view.findViewById(R.id.edittext_customfoul);
				customFoulType.setText("-");
				customFoulType.addTextChangedListener(new TextWatcher() {

					public void afterTextChanged(Editable s) {
					}

					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start, int before, int count) {
						if (!s.toString().startsWith("-")) {
							customFoulType.setText("-" + String.valueOf(customFoulType.getText()).replace("-", ""));
							customFoulType.setSelection(customFoulType.getText().length());
						}
						if (customFoulType.getSelectionStart() == 0) {
							customFoulType.setSelection(1);
						}
					}

				});
				foulTypeSelector.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (checkedId == R.id.radiobutton_customfoul) {
							customFoulType.setVisibility(View.VISIBLE);
							customFoulType.requestFocus();
							customFoulType.setSelection(customFoulType.getText().length());
						} else {
							customFoulType.setVisibility(View.INVISIBLE);
						}

						for (int i = 0; i < group.getChildCount(); i++) {
							((RadioButton) group.getChildAt(i)).setTextColor(Color.BLACK);
						}
						((RadioButton) view.findViewById(checkedId)).setTextColor(blue);
					}
				});

				dialog = builder.setTitle("Foul").setView(view).setPositiveButton("OK", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						int index = profileContainer.indexOfChild(profileContainer.findViewById(profileContainer.getCheckedRadioButtonId()));
						Log.d("foul profile index", index + "");

						int amount;
						switch (foulTypeSelector.indexOfChild(foulTypeSelector.findViewById(foulTypeSelector.getCheckedRadioButtonId()))) {
						case 0:
							amount = 1;
							break;
						case 1:
							amount = 2;
							break;
						case 2:
							amount = 15;
							break;
						case 3:
							try {
								amount = Integer.valueOf(String.valueOf(customFoulType.getText()).replace("-", ""));
							} catch (NumberFormatException e) {
								amount = 0;
							}
							break;
						default:
							amount = 0;
						}
						if (scoreAdapter.getCount() > 0) {
							scoreAdapter.getItem(scoreAdapter.getCount() - 1).addFoul(index, amount);
							scoreAdapter.getItem(scoreAdapter.getCount() - 1).reduceScore(index, amount);
						} else {
							int[] score = new int[game.getPlayerCount()];
							for (int i = 0; i < game.getPlayerCount(); i++) {
								score[i] = 0;
								if (i == index) {
									score[i] = -amount;
								}
							}
							game.setRound(game.getRound() + 1);
							scoreAdapter.add(new Round(game.getRemainingBalls(), 1, score, index));
							scoreAdapter.getItem(0).addFoul(index, amount);
						}
						game.getPlayers()[index].setScore(game.getPlayers()[index].getScore() - amount);
						scoreAdapter.notifyDataSetChanged();
						dialog.dismiss();
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create();
				dialog.show();

			}

		});
	}

	private void registerRerackButton() {

		rerackButton = (Button) findViewById(R.id.button_rerack);
		rerackButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (game.getRerackAddition() == 0) {
					savedRemainingBalls = game.getRemainingBalls();
				}
				rerack();
				registerRerackButton();
			}
		});

		rerackButton.setOnLongClickListener(new OnLongClickListener() {

			public boolean onLongClick(View v) {
				if (game.getRerackAddition() > 0) {
					unRerack();
					registerRerackButton();
				}
				return true;
			}
		});

		if (game.getRerackAddition() > 0) {
			rerackButton.setText("Re-rack (" + String.valueOf(game.getRerackAddition() / 14) + ")");
		} else {
			rerackButton.setText("Re-rack");
		}
	}

	private void registerPickerRemainingBalls() {
		pickerRemainingBalls = (NumberPicker) findViewById(R.id.picker_remaining_balls);
		pickerRemainingBalls.setMinValue(1);
		pickerRemainingBalls.setMaxValue(game.getRemainingBalls());
		pickerRemainingBalls.setWrapSelectorWheel(false);
		pickerRemainingBalls.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		pickerRemainingBalls.setValue(game.getRemainingBalls() - 1);
		pickerRemainingBalls.setValue(game.getRemainingBalls());

	}

	private void registerScoreList() {
		scoreList = (ListView) findViewById(R.id.listview_score);
		scoreAdapter = new ScoreAdapter(this, 0);
		scoreList.setAdapter(scoreAdapter);
		LayoutParams params = new LayoutParams((game.getPlayerCount() + 1) * LIST_COLUMN_WIDTH, LayoutParams.MATCH_PARENT);
		scoreList.setLayoutParams(params);
		scoreList.setDividerHeight(0);

		scoreList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
				Toast.makeText(GameActivity.this, getRoundSummary(arg2),
						scoreAdapter.getItem(arg2).getFouls().size() > 0 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
			}
		});

	}

	private void registerProfilesCaption() {
		profileContainer = (LinearLayout) findViewById(R.id.linearlayout_caption_profiles);

		LayoutParams containerParams = new LayoutParams((game.getPlayerCount() + 1) * LIST_COLUMN_WIDTH, LayoutParams.WRAP_CONTENT);
		containerParams.gravity = Gravity.CENTER_HORIZONTAL;
		profileContainer.setLayoutParams(containerParams);

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);

		TextView round = new TextView(this);
		round.setText("Round");
		round.setTextSize(18);
		round.setGravity(Gravity.BOTTOM | Gravity.LEFT);

		profileContainer.addView(round, params);

		for (final Profile profile : game.getPlayers()) {
			View view = LayoutInflater.from(this).inflate(R.layout.caption_profile, null);
			final Button profileView = (Button) view.findViewById(R.id.profile_button);
			profileView.setText(profile.getFirstName());
			profileView.setOnLongClickListener(new OnLongClickListener() {

				public boolean onLongClick(View v) {
					showProfileInfo(profile);
					return true;
				}
			});
			profileView.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					if (pickerRemainingBalls.getValue() > 1) {
						addRound(profile);
					} else {
						rerackMessage();
					}
				}

			});

			profileView.setFocusable(false);

			new PictureRetriever(profile.getFacebookId()) {

				@Override
				public void onPostExecute(HashMap<String, Object> bmp) {
					profileView.setCompoundDrawablesWithIntrinsicBounds(null, new BitmapDrawable(getResources(), (Bitmap) bmp.get("picture")), null, null);

					profileView.setContentDescription(profile.getFirstName() + " " + profile.getLastName());
				}
			}.execute();

			profileContainer.addView(view, params);
		}
	}
	
	public void rerackMessage() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog;
		dialog = builder.setTitle("Rerack").setMessage("When there is only 1 ball left on the table, you should re-rack before ending your turn.").setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).create();
		dialog.show();
	}

	private void showProfileInfo(Profile profile) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog;
		View view = LayoutInflater.from(this).inflate(R.layout.dialog_profileinfo, null);
		TextView name = (TextView) view.findViewById(R.id.value_name);
		TextView bday = (TextView) view.findViewById(R.id.value_birthday);
		TextView currentAverage = (TextView) view.findViewById(R.id.value_currentaverage);

		GregorianCalendar date = profile.getBirthday();
		String dateString = date.get(GregorianCalendar.DAY_OF_MONTH) + " "
				+ date.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG, Locale.US) + " " + date.get(GregorianCalendar.YEAR);

		name.setText(profile.getFirstName() + " " + profile.getLastName());
		bday.setText(dateString);
		currentAverage.setText(Double.toString(profile.getCurrentGameAverage()));

		dialog = builder.setTitle(profile.getFirstName()).setView(view).create();
		dialog.show();

	}

	// TODO end initialisation (no todo, just bookmark)

	private void resetPickerMax() {
		pickerRemainingBalls.setMaxValue(15);
		pickerRemainingBalls.setWrapSelectorWheel(false);
		pickerRemainingBalls.setValue(14);
		pickerRemainingBalls.setValue(15);
	}

	private void rerack() {
		game.rerack();
		registerPickerRemainingBalls();
		resetPickerMax();
	}

	private void unRerack() {
		game.resetReracks();
		game.setRemainingBalls(savedRemainingBalls);
		registerPickerRemainingBalls();
	}

	private ArrayList<Round> roundsAsList() {
		ArrayList<Round> rounds = new ArrayList<Round>();
		for (int i = 0; i < scoreAdapter.getCount(); i++) {
			rounds.add(scoreAdapter.getItem(i));
		}
		return rounds;
	}

	private void revert() {

		scoreAdapter.remove(scoreAdapter.getItem(scoreAdapter.getCount() - 1));

		if (scoreAdapter.getCount() == 0) {
			for (Profile p : game.getPlayers()) {
				p.setScore(0);
			}
			game.setRemainingBalls(15);
			registerPickerRemainingBalls();
			registerRerackButton();
			game.setRound(0);
		} else {

			LinearLayout newLastRow = (LinearLayout) scoreAdapter.getView(scoreAdapter.getCount() - 1, null, null);
			int count = newLastRow.getChildCount() - 1;
			for (int i = 0; i < count; i++) {
				game.getPlayers()[i].setScore(Integer.valueOf((String) ((TextView) newLastRow.getChildAt(i + 1)).getText()));
			}
			game.setRound(Integer.valueOf((String) ((TextView) newLastRow.getChildAt(0)).getText().toString().replace(" ", "")));
			game.setRemainingBalls(scoreAdapter.getItem(scoreAdapter.getCount() - 1).getRemainingBalls());

			registerPickerRemainingBalls();
			registerRerackButton();
		}

		game.decreaseTurnIndex();
	}

	private void addRound() {
		game.addRound();

		int turnIndex = game.getAndIncreaseTurnIndex();
		int score = (game.getRemainingBalls() - pickerRemainingBalls.getValue()) + game.getRerackAddition();

		game.getPlayers()[turnIndex].appendToScore(score);
		int[] scores = new int[game.getPlayerCount()];
		for (int i = 0; i < scores.length; i++) {
			scores[i] = game.getPlayers()[i].getScore();
		}

		scoreAdapter.add(new Round(pickerRemainingBalls.getValue(), game.getRound(), scores, turnIndex));

		game.setRemainingBalls(pickerRemainingBalls.getValue());
		game.resetReracks();

		registerRerackButton();
		registerPickerRemainingBalls();
	}

	private void addRound(Profile profile) {
		ArrayList<Profile> profiles = new ArrayList<Profile>();
		for (Profile p : game.getPlayers()) {
			profiles.add(p);
		}
		int turnIndex = profiles.indexOf(profile);
		game.addRound();

		game.setTurnIndex(turnIndex);
		game.getAndIncreaseTurnIndex();
		int score = (game.getRemainingBalls() - pickerRemainingBalls.getValue()) + game.getRerackAddition();

		game.getPlayers()[turnIndex].appendToScore(score);
		int[] scores = new int[game.getPlayerCount()];
		for (int i = 0; i < scores.length; i++) {
			scores[i] = game.getPlayers()[i].getScore();
		}

		scoreAdapter.add(new Round(pickerRemainingBalls.getValue(), game.getRound(), scores, turnIndex));

		game.setRemainingBalls(pickerRemainingBalls.getValue());
		game.resetReracks();

		registerRerackButton();
		registerPickerRemainingBalls();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.activity_main, menu);
		SharedPreferences settings = getSharedPreferences("settings", 0);
		menu.findItem(R.id.menu_save).setVisible(!settings.getBoolean("autosave", false));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_revert:
			if (scoreAdapter.getCount() > 0) {
				revert();
			} else {
				// TODO melding
			}

		}

		return false;

	}

	private class ScoreAdapter extends ArrayAdapter<Round> {

		int previousCount;

		public ScoreAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			previousCount = getCount();
		}

		@Override
		public boolean isEnabled(int p) {
			return true;
		}

		@Override
		public View getView(int p, View view, ViewGroup group) {
			view = new LinearLayout(GameActivity.this);

			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
			params.bottomMargin = (int) (getResources().getDimension(R.dimen.default_margin) / 2);
			params.topMargin = (int) (getResources().getDimension(R.dimen.default_margin) / 2);

			Round round = getItem(p);

			TextView number = new TextView(GameActivity.this);
			number.setText("   " + String.valueOf(round.getNumber()));
			number.setGravity(Gravity.LEFT);

			view.setContentDescription(getRoundSummary(p));

			((ViewGroup) view).addView(number, params);

			for (int score : round.getScore()) {
				TextView scoreView = new TextView(GameActivity.this);
				scoreView.setText(String.valueOf(score));
				scoreView.setGravity(Gravity.CENTER_HORIZONTAL);
				((ViewGroup) view).addView(scoreView, params);

			}

			if (p == getCount() - 1 && getCount() > previousCount) {
				Animation animation = AnimationUtils.loadAnimation(GameActivity.this, R.anim.slide_top_to_bottom);
				view.startAnimation(animation);
				previousCount = getCount();
			}

			return view;
		}

		@Override
		public void add(Round round) {
			super.add(round);
			scoreList.smoothScrollToPosition(getCount() - 1);

		}

	}

	public String getRoundSummary(int r) {
		Round thisRound = roundsAsList().get(r);
		int player = thisRound.getProfileIndex();
		String summary;
		int ballCount;
		if (r > 0) {
			Round prevRound = roundsAsList().get(r - 1);
			ballCount = thisRound.getScore()[player] - prevRound.getScore()[player] + (thisRound.getFouls().size() == 0 ? 0 : thisRound.getFoul(player));
		} else {
			ballCount = thisRound.getScore()[player] + (thisRound.getFouls().size() == 0 ? 0 : thisRound.getFoul(player));
			;
		}

		summary = "Round " + thisRound.getNumber() + ": " + thisRound.getRemainingBalls() + " balls left on table, "
				+ game.getPlayers()[player].getFirstName() + " potted " + ballCount + (ballCount == 1 ? " ball. " : " balls.");

		ArrayList<Foul> fouls = thisRound.getFouls();

		for (Foul f : fouls) {
			if (fouls.size() > 1) {
				if (fouls.indexOf(f) == 0) {
					summary += " Subtracted " + f.amount + " points from " + game.getPlayers()[f.index].getFirstName() + ", ";
				} else {
					summary += f.amount + " points from " + game.getPlayers()[f.index].getFirstName() + (fouls.indexOf(f) != fouls.size() - 1 ? ", " : "");
				}
				if (fouls.indexOf(f) == fouls.size() - 1) {
					summary += " for fouls.";
				}
			} else {
				summary += " Subtracted " + f.amount + " points from " + game.getPlayers()[f.index].getFirstName() + " for a foul.";
			}
		}

		return summary;
	}

}
