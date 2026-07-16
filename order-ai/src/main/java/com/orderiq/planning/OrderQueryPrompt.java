package com.orderiq.planning;

/** Provider-independent system and user messages for one SQL planning request. */
public record OrderQueryPrompt(String systemMessage, String userMessage) {

	public OrderQueryPrompt {
		systemMessage = requireText(systemMessage, "systemMessage");
		userMessage = requireText(userMessage, "userMessage");
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("%s must not be blank".formatted(field));
		}
		return value.strip();
	}
}
