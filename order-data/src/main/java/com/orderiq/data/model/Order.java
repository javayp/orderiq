package com.orderiq.data.model;

import com.orderiq.data.util.TextValues;

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
		orderId = TextValues.requireText(orderId, "orderId");
		customerId = TextValues.requireText(customerId, "customerId");
		Objects.requireNonNull(orderDate, "orderDate must not be null");
		Objects.requireNonNull(amountUsd, "amountUsd must not be null");
	}

}
