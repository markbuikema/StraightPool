package com.markbuikema.straightpool;

import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int LIST_COLUMN_WIDTH = 150;

	private NumberPicker pickerRemainingBalls;
	private ListView scoreList;
	private LinearLayout profileContainer;
	private Game game;
	private ScoreAdapter scoreAdapter;
	private int screenWidth;
	private int screenHeight;
	private int savedRemainingBalls = 1;

	private Button addButton;
	private Button rerackButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.getSerializable("game") != null) {
			game = (Game) getIntent().getExtras().getSerializable("game");
		} else {
			Toast.makeText(this, "Error: could not create game.",
					Toast.LENGTH_LONG).show();

			GregorianCalendar mid = new GregorianCalendar();
			mid.set(1994, 3, 10);
			GregorianCalendar oldest = new GregorianCalendar();
			oldest.set(1993, 3, 10);
			GregorianCalendar youngest = new GregorianCalendar();
			youngest.set(1995, 3, 10);

			game = new Game(new Profile[] {
					new Profile("oudst", "Buikema", oldest, true, null),
					new Profile("mid", "Buikema", mid, true, null),
					new Profile("jongst", "Buikema", youngest, true, null) });
		}
		setContentView(R.layout.activity_main);
		setScreenDimensions();
		registerProfilesCaption();
		registerPickerRemainingBalls();
		registerScoreList();
		registerAddButton();
		registerRerackButton();
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
			rerackButton.setText("Re-rack ("
					+ String.valueOf(game.getRerackAddition() / 14) + ")");
		} else {
			rerackButton.setText("Re-rack");
		}
	}

	private void registerPickerRemainingBalls() {
		pickerRemainingBalls = (NumberPicker) findViewById(R.id.picker_remaining_balls);
		pickerRemainingBalls.setMinValue(1);
		pickerRemainingBalls.setMaxValue(game.getRemainingBalls());
		pickerRemainingBalls.setWrapSelectorWheel(false);
		pickerRemainingBalls
				.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		pickerRemainingBalls
				.setOnValueChangedListener(new OnValueChangeListener() {

					public void onValueChange(NumberPicker picker, int oldVal,
							int newVal) {
						if (newVal == 1) {
							addButton.setEnabled(false);
						} else {
							addButton.setEnabled(true);
						}
					}
				});
		pickerRemainingBalls.setValue(game.getRemainingBalls() - 1);
		pickerRemainingBalls.setValue(game.getRemainingBalls());

	}

	private void registerScoreList() {
		scoreList = (ListView) findViewById(R.id.listview_score);
		scoreAdapter = new ScoreAdapter(this, 0);
		scoreList.setAdapter(scoreAdapter);
		LayoutParams params = new LayoutParams((game.getPlayerCount() + 1)
				* LIST_COLUMN_WIDTH, LayoutParams.MATCH_PARENT);
		scoreList.setLayoutParams(params);
		scoreList.setDividerHeight(0);

	}

	private void registerProfilesCaption() {
		profileContainer = (LinearLayout) findViewById(R.id.linearlayout_caption_profiles);

		LayoutParams containerParams = new LayoutParams(
				(game.getPlayerCount() + 1) * LIST_COLUMN_WIDTH,
				LayoutParams.WRAP_CONTENT);
		containerParams.gravity = Gravity.CENTER_HORIZONTAL;
		profileContainer.setLayoutParams(containerParams);

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT, 1.0f);

		TextView round = new TextView(this);
		round.setText("Round");
		round.setTextSize(18);
		round.setGravity(Gravity.BOTTOM | Gravity.LEFT);

		profileContainer.addView(round, params);

		for (Profile profile : game.getPlayers()) {
			View view = LayoutInflater.from(this).inflate(
					R.layout.caption_profile, null);
			Button profileView = (Button) view
					.findViewById(R.id.profile_button);
			profileView.setText(profile.getFirstName());
			if (profile.getPicture() != null) {
				profileView
						.setCompoundDrawablesWithIntrinsicBounds(
								null,
								new BitmapDrawable(getResources(), profile
										.getPicture()), null, null);
			} else {
				profileView
						.setCompoundDrawablesWithIntrinsicBounds(
								null,
								new BitmapDrawable(
										getResources(),
										BitmapFactory
												.decodeResource(
														getResources(),
														android.R.drawable.ic_menu_report_image)),
								null, null);
			}

			profileContainer.addView(view, params);
		}
	}

	private void registerAddButton() {
		addButton = (Button) findViewById(R.id.button_add);
		addButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				addRound();
				registerPickerRemainingBalls();
			}
		});
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
		addButton.setEnabled(true);
	}

	private void unRerack() {
		game.resetReracks();
		game.setRemainingBalls(savedRemainingBalls);
		registerPickerRemainingBalls();
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

			LinearLayout newLastRow = (LinearLayout) scoreAdapter.getView(
					scoreAdapter.getCount() - 1, null, null);
			int count = newLastRow.getChildCount() - 1;
			for (int i = 0; i < count; i++) {
				game.getPlayers()[i].setScore(Integer
						.valueOf((String) ((TextView) newLastRow
								.getChildAt(i + 1)).getText()));
			}
			game.setRound(Integer.valueOf((String) ((TextView) newLastRow
					.getChildAt(0)).getText()));
			game.setRemainingBalls(scoreAdapter.getItem(
					scoreAdapter.getCount() - 1).getRemainingBalls());

			registerPickerRemainingBalls();
			registerRerackButton();
		}

		game.decreaseTurnIndex();
	}

	private void addRound() {
		game.addRound();

		int turnIndex = game.getAndIncreaseTurnIndex();

		game.getPlayers()[turnIndex]
				.appendToScore((game.getRemainingBalls() - pickerRemainingBalls
						.getValue()) + game.getRerackAddition());
		int[] scores = new int[game.getPlayerCount()];
		for (int i = 0; i < scores.length; i++) {
			scores[i] = game.getPlayers()[i].getScore();
		}
		scoreAdapter.add(new Round(pickerRemainingBalls.getValue(), game
				.getRound(), scores));

		game.setRemainingBalls(pickerRemainingBalls.getValue());
		game.resetReracks();

		registerRerackButton();
		registerPickerRemainingBalls();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.activity_main, menu);
		SharedPreferences settings = getSharedPreferences("settings", 0);
		menu.findItem(R.id.menu_save).setVisible(
				!settings.getBoolean("autosave", false));
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
			return false;
		}

		@Override
		public View getView(int p, View view, ViewGroup group) {
			view = new LinearLayout(MainActivity.this);

			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT, 1);

			Round round = getItem(p);

			TextView number = new TextView(MainActivity.this);
			number.setText(String.valueOf(round.getNumber()));
			number.setGravity(Gravity.LEFT);

			((ViewGroup) view).addView(number, params);

			for (int score : round.getScore()) {
				TextView scoreView = new TextView(MainActivity.this);
				scoreView.setText(String.valueOf(score));
				scoreView.setGravity(Gravity.CENTER_HORIZONTAL);
				((ViewGroup) view).addView(scoreView, params);

			}

			if (p == getCount() - 1 && getCount() > previousCount) {
				Animation animation = AnimationUtils.loadAnimation(
						MainActivity.this, R.anim.slide_top_to_bottom);
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

}
