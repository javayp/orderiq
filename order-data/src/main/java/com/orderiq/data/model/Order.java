package com.orderiq.data.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** A normalized order ready to be persisted and queried. */
public record Order(
		String orderId,
		String customerId,
		LocalDate orderDate,
		BigDecimal amountUsd) {

	public Order {
		orderId = requireText(orderId, "orderId");
		customerId = requireText(customerId, "customerId");
		Objects.requireNonNull(orderDate, "orderDate must not be null");
		Objects.requireNonNull(amountUsd, "amountUsd must not be null");
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("%s must not be blank".formatted(field));
		}
		return value;
	}
}
