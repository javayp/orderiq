package com.orderiq.guardrail;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	static <T> Set<T> matchingKeys(String text, Map<T, List<String>> rules) {
		Set<T> matches = new LinkedHashSet<>();
		for (Map.Entry<T, List<String>> rule : rules.entrySet()) {
			if (contains(text, rule.getValue())) {
				matches.add(rule.getKey());
			}
		}
		return matches;
	}

	static <T> T firstMatchingKey(String text, Map<T, List<String>> rules, T fallback) {
		for (Map.Entry<T, List<String>> rule : rules.entrySet()) {
			if (contains(text, rule.getValue())) {
				return rule.getKey();
			}
		}
		return fallback;
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
