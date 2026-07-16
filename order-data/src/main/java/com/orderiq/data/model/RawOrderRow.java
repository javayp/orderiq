package com.orderiq.data.model;

public record RawOrderRow(
		long rowNumber,
		String orderId,
		String customerId,
		String orderDate,
		String amount,
		String currency) {
}
