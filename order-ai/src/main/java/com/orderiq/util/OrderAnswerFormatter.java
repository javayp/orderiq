package com.orderiq.util;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class OrderAnswerFormatter {

	private OrderAnswerFormatter() {
	}

	public static String format(List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return "No matching orders found.";
		}
		if (rows.size() == 1) {
			return formatRow(rows.getFirst());
		}
		return "Found %d matching results.".formatted(rows.size());
	}

	private static String formatRow(Map<String, Object> row) {
		StringJoiner answer = new StringJoiner(", ");
		for (Map.Entry<String, Object> field : row.entrySet()) {
			answer.add("%s: %s".formatted(humanize(field.getKey()), field.getValue()));
		}
		return answer.toString();
	}

	private static String humanize(String field) {
		String label = field.replace('_', ' ');
		String capitalized = Character.toUpperCase(label.charAt(0)) + label.substring(1);
		return capitalized.replace(" id", " ID").replace(" usd", " USD");
	}
}
