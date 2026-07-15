package com.orderiq.guardrail;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalization and structural operations shared while analysing a question. */
final class QuestionText {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern SPACED_LETTERS = Pattern.compile(
			"(?<![a-z0-9])(?:[a-z]\\s+){2,}[a-z](?![a-z0-9])");
	private static final Pattern DIGIT = Pattern.compile("\\d");
	private static final Pattern QUESTION_DELIMITER = Pattern.compile("[?;!\\r\\n]+");
	private static final Pattern TRAILING_DELIMITER = Pattern.compile("[?;!]+\\s*$");

	private QuestionText() {
	}

	static String require(String question) {
		if (question == null || question.isBlank()) {
			throw new IllegalArgumentException("question must not be blank");
		}
		return question;
	}

	static String normalize(String question) {
		String normalized = Normalizer.normalize(question, Normalizer.Form.NFKC)
				.replace('’', '\'')
				.strip()
				.toLowerCase(Locale.ROOT);
		return WHITESPACE.matcher(normalized).replaceAll(" ");
	}

	static String securityText(String normalizedQuestion) {
		Matcher matcher = SPACED_LETTERS.matcher(normalizedQuestion);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(
					result,
					Matcher.quoteReplacement(matcher.group().replace(" ", "")));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	static String[] clauses(String question) {
		String stripped = question.strip();
		String withoutTrailingDelimiter = TRAILING_DELIMITER.matcher(stripped).replaceFirst("");
		return QUESTION_DELIMITER.split(withoutTrailingDelimiter);
	}

	static boolean containsDigit(String text) {
		return DIGIT.matcher(text).find();
	}
}
