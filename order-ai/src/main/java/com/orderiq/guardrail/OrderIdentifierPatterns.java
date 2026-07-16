package com.orderiq.guardrail;

import java.util.List;
import java.util.regex.Pattern;

/** Owns the supported customer and order identifier syntax. */
final class OrderIdentifierPatterns {

	private static final String CUSTOMER_ID =
			"(?=[a-z0-9_-]{2,32}\\b)(?=[a-z0-9_-]*\\d)[a-z0-9][a-z0-9_-]{1,31}";
	private static final String RECOGNIZABLE_CUSTOMER_ID = "[a-z]{2}-\\d{5}|[a-z]\\d{3,}";
	private static final String CUSTOMER_ID_PATTERN_TEMPLATE =
			"(?i)(?:\\b(?:%s)(?:\\s+(?:id|number))?\\s*(?:[:=#]\\s*)?(%s)\\b|\\b(%s)\\b)";
	private static final String ORDER_ID_PATTERN_TEMPLATE =
			"(?i)\\b(?:%s)(?:\\s+(?:id|number))?\\s*(?:[:=#]\\s*)?(\\d{3,20})\\b";

	private OrderIdentifierPatterns() {
	}

	static Pattern customerId(List<String> labels) {
		return Pattern.compile(CUSTOMER_ID_PATTERN_TEMPLATE.formatted(
				alternation(labels),
				CUSTOMER_ID,
				RECOGNIZABLE_CUSTOMER_ID));
	}

	static Pattern orderId(List<String> labels) {
		return Pattern.compile(ORDER_ID_PATTERN_TEMPLATE.formatted(alternation(labels)));
	}

	private static String alternation(List<String> phrases) {
		StringBuilder expression = new StringBuilder();
		for (String phrase : phrases) {
			if (!expression.isEmpty()) {
				expression.append('|');
			}
			expression.append(Pattern.quote(phrase));
		}
		return expression.toString();
	}
}
