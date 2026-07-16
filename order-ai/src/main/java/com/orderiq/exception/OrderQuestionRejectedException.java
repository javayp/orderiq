package com.orderiq.exception;

import com.orderiq.guardrail.OrderQueryFrame.Decision;

public final class OrderQuestionRejectedException extends RuntimeException {

	private final Decision decision;

	public OrderQuestionRejectedException(Decision decision, String message) {
		super(message);
		this.decision = decision;
	}

	public Decision decision() {
		return decision;
	}
}
