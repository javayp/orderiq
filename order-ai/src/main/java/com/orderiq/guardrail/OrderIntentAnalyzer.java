package com.orderiq.guardrail;

import com.orderiq.guardrail.OrderQueryFrame.Grouping;
import com.orderiq.guardrail.OrderQueryFrame.Metric;
import com.orderiq.guardrail.OrderQueryFrame.Sort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts order-domain evidence without deciding whether a question is allowed. */
@Component
public final class OrderIntentAnalyzer {

	private final OrderVocabularyConfiguration configuration;
	private final Pattern customerIdPattern;
	private final Pattern orderIdPattern;

	public OrderIntentAnalyzer(OrderVocabularyConfiguration configuration) {
		this.configuration = configuration;
		customerIdPattern = OrderIdentifierPatterns.customerId(configuration.customerIdLabels());
		orderIdPattern = OrderIdentifierPatterns.orderId(configuration.orderIdLabels());
	}

	OrderIntentEvidence analyze(String original, String normalized) {
		Set<Metric> metrics = VocabularyMatcher.matchingKeys(normalized, configuration.metrics());
		Set<Grouping> groupings = VocabularyMatcher.matchingKeys(normalized, configuration.groupings());
		Sort sort = VocabularyMatcher.firstMatchingKey(
				normalized,
				configuration.sorting(),
				Sort.NONE);
		List<String> customerIds = customerIds(original);
		Optional<String> orderId = orderId(original);
		List<String> unsupported = List.copyOf(
				VocabularyMatcher.matchingKeys(normalized, configuration.unsupported()));

		addCustomerGrouping(normalized, metrics, groupings, customerIds);
		boolean recognized = isRecognized(normalized, metrics, groupings, sort, customerIds, orderId);
		if (recognized && metrics.isEmpty()) {
			metrics.add(groupings.isEmpty() ? Metric.ORDER_LIST : Metric.ORDER_COUNT);
		}

		return new OrderIntentEvidence(
				metrics,
				groupings,
				sort,
				customerIds,
				orderId,
				unsupported,
				recognized);
	}

	boolean hasOrderEvidence(String original, String normalized) {
		return !customerIds(original).isEmpty()
				|| orderId(original).isPresent()
				|| !VocabularyMatcher.matchingKeys(normalized, configuration.metrics()).isEmpty()
				|| !VocabularyMatcher.matchingKeys(normalized, configuration.groupings()).isEmpty()
				|| VocabularyMatcher.firstMatchingKey(
						normalized,
						configuration.sorting(),
						Sort.NONE) != Sort.NONE
				|| VocabularyMatcher.contains(normalized, configuration.anchors())
				|| !VocabularyMatcher.matchingKeys(normalized, configuration.unsupported()).isEmpty();
	}

	private void addCustomerGrouping(
			String question,
			Set<Metric> metrics,
			Set<Grouping> groupings,
			List<String> customerIds) {
		if (customerIds.isEmpty()
				&& !metrics.isEmpty()
				&& VocabularyMatcher.contains(question, configuration.customerGroupingHints())) {
			groupings.add(Grouping.CUSTOMER);
		}
	}

	private boolean isRecognized(
			String question,
			Set<Metric> metrics,
			Set<Grouping> groupings,
			Sort sort,
			List<String> customerIds,
			Optional<String> orderId) {
		boolean strongEvidence = !customerIds.isEmpty() || orderId.isPresent() || !metrics.isEmpty();
		boolean contextualEvidence = VocabularyMatcher.contains(question, configuration.anchors())
				&& (VocabularyMatcher.contains(question, configuration.contextHints())
						|| !groupings.isEmpty()
						|| sort != Sort.NONE);
		return strongEvidence || contextualEvidence;
	}

	private List<String> customerIds(String question) {
		Map<String, String> uniqueIds = new LinkedHashMap<>();
		Matcher matcher = customerIdPattern.matcher(question);
		while (matcher.find()) {
			String customerId = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
			uniqueIds.putIfAbsent(customerId.toLowerCase(Locale.ROOT), customerId);
		}
		return List.copyOf(uniqueIds.values());
	}

	private Optional<String> orderId(String question) {
		Matcher matcher = orderIdPattern.matcher(question);
		return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
	}

}
