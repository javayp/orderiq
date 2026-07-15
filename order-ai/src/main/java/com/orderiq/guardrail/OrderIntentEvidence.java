package com.orderiq.guardrail;

import com.orderiq.guardrail.OrderQueryFrame.Grouping;
import com.orderiq.guardrail.OrderQueryFrame.Metric;
import com.orderiq.guardrail.OrderQueryFrame.Sort;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Immutable domain evidence extracted from one order question. */
record OrderIntentEvidence(
		Set<Metric> metrics,
		Set<Grouping> groupings,
		Sort sort,
		List<String> customerIds,
		Optional<String> orderId,
		List<String> unsupported,
		boolean recognized) {

	OrderIntentEvidence {
		metrics = Set.copyOf(metrics);
		groupings = Set.copyOf(groupings);
		customerIds = List.copyOf(customerIds);
		unsupported = List.copyOf(unsupported);
	}
}
