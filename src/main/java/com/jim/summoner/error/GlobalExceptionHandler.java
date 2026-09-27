package com.jim.summoner.error;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(GameDataNotFoundException.class)
	public ResponseEntity<Map<String, Object>>
			handleGameDataNotFound(
					GameDataNotFoundException e) {

		return ResponseEntity.status(
				HttpStatus.NOT_FOUND
		).body(
				Map.of(
						"status", 404,
						"message", e.getMessage()
				)
		);
	}
}