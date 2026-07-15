package com.orderiq.guardrail;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared boundary-aware matching for externally configured phrases. */
final class VocabularyMatcher {

	private VocabularyMatcher() {
	}

	static boolean contains(String text, List<String> phrases) {
		for (String phrase : phrases) {
			if (pattern(phrase).matcher(text).find()) {
				return true;
			}
		}
		return false;
	}

	static boolean endsWith(String text, List<String> phrases) {
		for (String phrase : phrases) {
			if (text.equals(phrase) || text.endsWith(" ".concat(phrase))) {
				return true;
			}
		}
		return false;
	}

	static int nextMatchEnd(String text, String phrase, int fromIndex) {
		Matcher matcher = pattern(phrase).matcher(text);
		return matcher.find(fromIndex) ? matcher.end() : -1;
	}

	private static Pattern pattern(String phrase) {
		int first = phrase.codePointAt(0);
		int last = phrase.codePointBefore(phrase.length());
		String leadingBoundary = isWordCharacter(first) ? "(?<![\\p{L}\\p{N}_])" : "";
		String trailingBoundary = isWordCharacter(last) ? "(?![\\p{L}\\p{N}_])" : "";
		return Pattern.compile(
				"%s%s%s".formatted(leadingBoundary, Pattern.quote(phrase), trailingBoundary),
				Pattern.UNICODE_CASE);
	}

	private static boolean isWordCharacter(int codePoint) {
		return Character.isLetterOrDigit(codePoint) || codePoint == '_';
	}
}
