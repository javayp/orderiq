package com.orderiq.guardrail;

import com.orderiq.guardrail.OrderQueryFrame.Grouping;
import com.orderiq.guardrail.OrderQueryFrame.Metric;
import com.orderiq.guardrail.OrderQueryFrame.Sort;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "orderiq.ai.guardrail.vocabulary")
public record OrderVocabularyConfiguration(
		Map<Metric, List<String>> metrics,
		Map<Grouping, List<String>> groupings,
		Map<Sort, List<String>> sorting,
		Map<String, List<String>> unsupported,
		List<String> anchors,
		List<String> contextHints,
		List<String> customerGroupingHints,
		List<String> customerIdLabels,
		List<String> orderIdLabels,
		List<String> blockedContent,
		List<String> securityIndicators,
		List<String> multipleQuestionMarkers,
		List<String> continuationMarkers,
		List<String> incompleteSuffixes) {

	public OrderVocabularyConfiguration {
		metrics = rules(metrics, "metrics");
		groupings = rules(groupings, "groupings");
		sorting = rules(sorting, "sorting");
		unsupported = namedRules(unsupported);
		anchors = phrases(anchors, "anchors");
		contextHints = phrases(contextHints, "context-hints");
		customerGroupingHints = phrases(customerGroupingHints, "customer-grouping-hints");
		customerIdLabels = phrases(customerIdLabels, "customer-id-labels");
		orderIdLabels = phrases(orderIdLabels, "order-id-labels");
		blockedContent = phrases(blockedContent, "blocked-content");
		securityIndicators = phrases(securityIndicators, "security-indicators");
		multipleQuestionMarkers = phrases(multipleQuestionMarkers, "multiple-question-markers");
		continuationMarkers = phrases(continuationMarkers, "continuation-markers");
		incompleteSuffixes = phrases(incompleteSuffixes, "incomplete-suffixes");
	}

	private static <K> Map<K, List<String>> rules(Map<K, List<String>> configured, String field) {
		if (configured == null || configured.isEmpty()) {
			throw new IllegalArgumentException("%s must not be empty".formatted(field));
		}
		Map<K, List<String>> copy = new LinkedHashMap<>();
		for (Map.Entry<K, List<String>> entry : configured.entrySet()) {
			copy.put(
					entry.getKey(),
					phrases(entry.getValue(), "%s.%s".formatted(field, entry.getKey())));
		}
		return Collections.unmodifiableMap(copy);
	}

	private static Map<String, List<String>> namedRules(Map<String, List<String>> configured) {
		Map<String, List<String>> copy = new LinkedHashMap<>();
		if (configured != null) {
			for (Map.Entry<String, List<String>> entry : configured.entrySet()) {
				String key = entry.getKey();
				copy.put(
						normalize(key).replace('-', ' '),
						phrases(entry.getValue(), "unsupported.%s".formatted(key)));
			}
		}
		return Collections.unmodifiableMap(copy);
	}

	private static List<String> phrases(List<String> configured, String field) {
		if (configured == null || configured.isEmpty()) {
			throw new IllegalArgumentException("%s must not be empty".formatted(field));
		}
		LinkedHashSet<String> values = new LinkedHashSet<>();
		for (String value : configured) {
			values.add(normalize(value));
		}
		if (values.contains("")) {
			throw new IllegalArgumentException("%s contains a blank phrase".formatted(field));
		}
		return List.copyOf(values);
	}

	private static String normalize(String value) {
		return value == null ? "" : QuestionText.normalize(value);
	}
}
