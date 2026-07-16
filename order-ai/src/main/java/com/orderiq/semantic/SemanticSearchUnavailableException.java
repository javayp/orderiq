package com.orderiq.semantic;

public final class SemanticSearchUnavailableException extends RuntimeException {

	public SemanticSearchUnavailableException(String message) {
		super(message);
	}

	public SemanticSearchUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
