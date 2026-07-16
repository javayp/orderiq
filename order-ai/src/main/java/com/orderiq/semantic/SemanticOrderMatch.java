package com.orderiq.semantic;

import com.orderiq.data.model.Order;

import java.util.Objects;

public record SemanticOrderMatch(Order order, double score) {

	public SemanticOrderMatch {
		Objects.requireNonNull(order, "order must not be null");
	}
}
