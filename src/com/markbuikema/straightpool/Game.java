package com.markbuikema.straightpool;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Game implements Serializable {

	
	private int playerCount;
	private Profile[] players;
	private int remainingBalls;
	private int round;
	private int turnIndex;
	private int rerackAddition;
	
	public Game(Profile[] players) {
		playerCount = players.length;
		this.players = players;
		remainingBalls = 15;
		for (Profile player: players) {
			player.setScore(0);
		}
		round = 0;
		
		turnIndex = getYoungestPlayerIndex();

		rerackAddition = 0;
	}

	private int getYoungestPlayerIndex() {

		//assert playerCount > 0
		
		Profile youngest = players[0];
		for (int i = 1; i < playerCount; i++) {
			if (players[i].getBirthday().getTimeInMillis() > youngest.getBirthday().getTimeInMillis()) {
				youngest = players[i];
			}
		}
		for (int i = 0; i < playerCount; i++) {
			if (youngest == players[i]) {
				return i;
			}
		}
		return 0;
	}

	public int getRemainingBalls() {
		return remainingBalls;
	}

	public void setRemainingBalls(int remainingBalls) {
		this.remainingBalls = remainingBalls;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public Profile[] getPlayers() {
		return players;
	}
	
	public void addRound() {
		round++;
	}
	
	public int getRound() {
		return round;
	}
	
	public int getRerackAddition() {
		return rerackAddition;
	}

	public void rerack() {
//		remainingBalls = 15;
		rerackAddition += 14;
	}
	
	public void resetReracks() {
		rerackAddition = 0;
	}

	public int getTurnIndex() {
		return turnIndex;
	}
	
	public int getAndIncreaseTurnIndex() {
		int index = turnIndex;
		if (turnIndex < playerCount-1) {
			turnIndex++;
		} else {
			turnIndex = 0;
		}
		return index;
	}
	
	public void decreaseTurnIndex() {
		int index = turnIndex;
		if (turnIndex > 0 ) {
			turnIndex--;
		} else {
			turnIndex = playerCount-1;
		}
	}

	public void setRound(int round) {
		this.round = round;
	}
	
}
