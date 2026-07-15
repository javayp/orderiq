package com.orderiq.data.policy.impl;

import com.orderiq.data.policy.OrderDateParser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class FlexibleOrderDateParser implements OrderDateParser {

	private static final List<DateTimeFormatter> ACCEPTED_FORMATS = List.of(
			DateTimeFormatter.ISO_LOCAL_DATE,
			strictFormatter("M/d/uuuu"),
			strictFormatter("d-M-uuuu"),
			strictFormatter("uuuu/M/d")
	);

	@Override
	public Optional<LocalDate> parse(String value) {
		String candidate = trimToNull(value);
		if (candidate == null) {
			return Optional.empty();
		}
		for (DateTimeFormatter formatter : ACCEPTED_FORMATS) {
			try {
				return Optional.of(LocalDate.parse(candidate, formatter));
			} catch (DateTimeParseException ignored) {
				// Try the next explicitly supported input format.
			}
		}
		return Optional.empty();
	}

	private static DateTimeFormatter strictFormatter(String pattern) {
		return DateTimeFormatter.ofPattern(pattern, Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
