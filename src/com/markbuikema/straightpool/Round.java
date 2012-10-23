package com.markbuikema.straightpool;

public class Round {

	private int number;
	private int[] score;
	private int remainingBalls;
	
	public Round(int remainingBalls, int number, int[] score) {
		this.number = number;
		this.score = score;
		this.remainingBalls = remainingBalls;
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
	
	public void reduceScore(int index, int amount) {
		score[index] -= amount;
	}
	
	
}
