package com.orderiq.planning;

import com.orderiq.data.exception.InvalidOrderQueryException;

import java.util.Objects;

public final class OrderQueryPlanValidator {

	private OrderQueryPlanValidator() {
	}

	public static void requireExecutable(OrderQueryPlan plan) {
		Objects.requireNonNull(plan, "Order query plan must not be null");
		if (plan.status() == OrderQueryPlan.Status.REJECTED) {
			String reason = plan.reason() == null || plan.reason().isBlank()
					? "The question cannot be answered from the orders schema."
					: plan.reason();
			throw new InvalidOrderQueryException(reason);
		}
		if (plan.status() != OrderQueryPlan.Status.QUERY) {
			throw new IllegalStateException("Order query plan has no status");
		}
	}
}
