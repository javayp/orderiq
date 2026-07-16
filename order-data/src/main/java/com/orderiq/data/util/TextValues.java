package com.orderiq.data.util;

public final class TextValues {

	private TextValues() {
	}

	public static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public static String requireText(String value, String field) {
		if (trimToNull(value) == null) {
			throw new IllegalArgumentException("%s must not be blank".formatted(field));
		}
		return value;
	}
}
