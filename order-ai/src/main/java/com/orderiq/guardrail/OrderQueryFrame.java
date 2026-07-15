package com.orderiq.guardrail;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Complete allow/reject decision and the order hints needed by the LLM. */
public record OrderQueryFrame(
		String originalQuestion,
		String normalizedQuestion,
		Decision decision,
		Set<Metric> metrics,
		Set<Grouping> groupings,
		Sort sort,
		List<String> customerIds,
		Optional<String> orderId,
		List<String> unsupportedTerms,
		String reason) {

	public OrderQueryFrame {
		Objects.requireNonNull(decision, "decision must not be null");
		metrics = Set.copyOf(metrics);
		groupings = Set.copyOf(groupings);
		Objects.requireNonNull(sort, "sort must not be null");
		customerIds = List.copyOf(customerIds);
		orderId = Objects.requireNonNull(orderId, "orderId must not be null");
		unsupportedTerms = List.copyOf(unsupportedTerms);
		Objects.requireNonNull(reason, "reason must not be null");
	}

	public boolean allowed() {
		return decision == Decision.ALLOWED;
	}

	static OrderQueryFrame rejected(
			String originalQuestion,
			String normalizedQuestion,
			Decision decision,
			String reason) {
		return new OrderQueryFrame(
				originalQuestion,
				normalizedQuestion,
				decision,
				Set.of(),
				Set.of(),
				Sort.NONE,
				List.of(),
				Optional.empty(),
				List.of(),
				reason);
	}

	public enum Decision {
		ALLOWED,
		OUT_OF_DOMAIN,
		UNSUPPORTED_SCHEMA,
		CONTENT_REJECTED,
		SECURITY_REJECTED,
		MULTIPLE_QUESTIONS,
		INCOMPLETE_QUESTION
	}

	public enum Metric {
		ORDER_LIST,
		TOTAL_REVENUE,
		AVERAGE_ORDER_VALUE,
		ORDER_COUNT
	}

	public enum Grouping {
		DAY,
		MONTH,
		YEAR,
		CUSTOMER
	}

	public enum Sort {
		NONE,
		ORDER_DATE_ASC,
		ORDER_DATE_DESC,
		AMOUNT_ASC,
		AMOUNT_DESC
	}
}
