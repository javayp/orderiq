package com.orderiq.api.model;

import com.orderiq.data.model.Order;
import com.orderiq.semantic.SemanticOrderMatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record SemanticOrderResponse(
		String orderId,
		String customerId,
		BigDecimal amountUsd,
		LocalDate orderDate,
		double score) {

	public static SemanticOrderResponse from(SemanticOrderMatch match) {
		Order order = match.order();
		return new SemanticOrderResponse(
				order.orderId(),
				order.customerId(),
				order.amountUsd(),
				order.orderDate(),
				BigDecimal.valueOf(match.score())
						.setScale(6, RoundingMode.HALF_UP)
						.doubleValue());
	}
}
