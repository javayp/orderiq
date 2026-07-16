package com.orderiq.util;

public final class TextSupport {

	private TextSupport() {
	}

	public static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("%s must not be blank".formatted(field));
		}
		return value.strip();
	}

	public static String singleLine(String value) {
		return value == null ? "" : value.replaceAll("[\\r\\n\\t]", " ");
	}

	public static String boundedSingleLine(String value, int maximumLength, String fallback) {
		String sanitized = singleLine(value).strip();
		if (sanitized.isEmpty()) {
			return fallback;
		}
		return sanitized.length() <= maximumLength
				? sanitized
				: sanitized.substring(0, maximumLength);
	}
}
