package com.markbuikema.straightpool;

import java.util.GregorianCalendar;

import android.graphics.Bitmap;

public class Profile {

	private String firstName;
	private String lastName;
	private GregorianCalendar birthday;
	private boolean isMale;
	private String facebookId;
	private String twitterId;
	private Bitmap picture;
	private int score;
	
	public Profile(String firstName, String lastName,
			GregorianCalendar birthday, boolean isMale, Bitmap picture) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthday = birthday;
		this.isMale = isMale;
		this.picture = picture;
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

	public boolean isMale() {
		return isMale;
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
