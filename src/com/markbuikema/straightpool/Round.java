package com.markbuikema.straightpool;

import java.util.ArrayList;

public class Round {

	private int number;
	private int[] score;
	private int remainingBalls;
	private int profileIndex;
	private ArrayList<Foul> fouls;

	public Round(int remainingBalls, int number, int[] score, int profileIndex) {
		this.number = number;
		this.score = score;
		this.remainingBalls = remainingBalls;
		this.profileIndex = profileIndex;

		fouls = new ArrayList<Foul>();
	}

	public void addFoul(int index, int amount) {

		for (Foul f : fouls) {
			if (f.index == index) {
				f.amount += amount;
				return;
			}
		}
		fouls.add(new Foul(index, amount));

	}

	public ArrayList<Foul> getFouls() {
		return fouls;
	}

	public int getProfileIndex() {
		return profileIndex;
	}

	public int getNumber() {
		return number;
	}

	public int[] getScore() {
		return score;
	}

	public int getRemainingBalls() {
		return remainingBalls;
	}

	public class Foul {
		public int index;
		public int amount;

		public Foul(int index, int amount) {
			this.index = index;
			this.amount = amount;
		}
	}

	public void reduceScore(int index, int amount) {
		score[index] -= amount;

	}

	public int getFoul(int index) {
		for (Foul f : fouls) {
			if (f.index == index) {
				return f.amount;
			}
		}
		return 0;
	}

}
