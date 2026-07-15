package com.orderiq.data.exception;

public final class IngestionException extends RuntimeException {

	public IngestionException(String message) {
		super(message);
	}

	public IngestionException(String message, Throwable cause) {
		super(message, cause);
	}
}
