package com.markbuikema.straightpool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;

public class Profile implements Serializable{

	private String firstName;
	private String lastName;
	private GregorianCalendar birthday;
	private String facebookId;
	private String twitterId;
	private int score;
	private String id;
	private double currentGameAverage;
	private ArrayList<Integer> currentGameScores;

	public Profile(String id, String firstName, String lastName, GregorianCalendar birthday, String facebookId, String twitterId) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthday = birthday;
		this.facebookId = facebookId;
		this.twitterId = twitterId;
		currentGameAverage = 0.0;
		currentGameScores = new ArrayList<Integer>();
	}

	public double getCurrentGameAverage() {
		return currentGameAverage;
	}

	public void setCurrentGameAverage(double currentGameAverage) {
		this.currentGameAverage = currentGameAverage;
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

	public String getId() {
		return id;
	}

	public void appendToCurrentGameAverage(int score) {
		currentGameScores.add(score);
		updateCurrentGameAverage();
	}

	public void removeLastScoreFromCurrentGameAverage() {
		int index = currentGameScores.size() - 1;
		currentGameScores.remove(index);
		updateCurrentGameAverage();
	}

	private void updateCurrentGameAverage() {
		double sum = 0;
		for (double s : currentGameScores) {
			sum += s;
		}
		if (currentGameScores.size() != 0) {
			currentGameAverage = sum / currentGameScores.size();
		} else {
			currentGameAverage = 0;
		}
	}

}
