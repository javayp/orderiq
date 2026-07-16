package com.orderiq.api.model;

import com.orderiq.data.model.Order;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderResponse(
		String orderId,
		String customerId,
		LocalDate orderDate,
		BigDecimal amountUsd) {

	public static OrderResponse from(Order order) {
		return new OrderResponse(
				order.orderId(), order.customerId(), order.orderDate(), order.amountUsd());
	}
}
