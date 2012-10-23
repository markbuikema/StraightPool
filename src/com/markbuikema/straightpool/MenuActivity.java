package com.markbuikema.straightpool;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MenuActivity extends Activity {

	Button newGameButton;
	Button settingsButton;
	Button profilesButton;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		newGameButton = (Button) findViewById(R.id.button_new_game);
		settingsButton = (Button) findViewById(R.id.button_settings);
		profilesButton = (Button) findViewById(R.id.button_profiles);

		newGameButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent i = new Intent(MenuActivity.this, CreateGameActivity.class);
				i.setAction(Intent.ACTION_VIEW);
				startActivity(i);
			}
		});

		settingsButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent i = new Intent(MenuActivity.this, SettingsActivity.class);
				i.setAction(Intent.ACTION_VIEW);
				startActivity(i);
			}
		});
		profilesButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent i = new Intent(MenuActivity.this, ProfileManagerActivity.class);
				i.setAction(Intent.ACTION_VIEW);
				startActivity(i);
			}
		});

	}
}
