package com.jim.summoner.error;

public class GameDataNotFoundException
		extends RuntimeException {

	public GameDataNotFoundException(
			String message) {

		super(message);
	}
}