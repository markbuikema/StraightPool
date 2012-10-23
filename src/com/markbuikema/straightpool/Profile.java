package com.markbuikema.straightpool;

import java.util.GregorianCalendar;

import android.graphics.Bitmap;

public class Profile {

	private String firstName;
	private String lastName;
	private GregorianCalendar birthday;
	private String facebookId;
	private String twitterId;
	private Bitmap picture;
	private String pictureUrl;
	private int score;

	public Profile(String firstName, String lastName, GregorianCalendar birthday, String pictureUrl, Bitmap picture, String facebookId, String twitterId) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthday = birthday;
		this.picture = picture;
		this.facebookId = facebookId;
		this.twitterId = twitterId;
		this.pictureUrl = pictureUrl;
	}

	public String getPictureUrl() {
		return pictureUrl;
	}


	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public void appendToScore(int score) {
		this.score += score;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public GregorianCalendar getBirthday() {
		return birthday;
	}

	public String getFacebookId() {
		return facebookId;
	}

	public String getTwitterId() {
		return twitterId;
	}

	public Bitmap getPicture() {
		return picture;
	}

}
