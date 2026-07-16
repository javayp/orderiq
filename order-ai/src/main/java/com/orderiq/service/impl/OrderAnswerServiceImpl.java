package com.orderiq.service.impl;

import com.orderiq.guardrail.OrderQueryFrame;
import com.orderiq.guardrail.OrderQuestionGuardrail;
import com.orderiq.api.model.AskOrderRequest;
import com.orderiq.api.model.AskOrderResponse;
import com.orderiq.exception.OrderQuestionRejectedException;
import com.orderiq.planning.OrderQueryPlanner;
import com.orderiq.service.OrderAnswerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public final class OrderAnswerServiceImpl implements OrderAnswerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderAnswerServiceImpl.class);

	private final OrderQuestionGuardrail orderQuestionGuardrail;
	private final OrderQueryPlanner orderQueryPlanner;

	public OrderAnswerServiceImpl(OrderQuestionGuardrail orderQuestionGuardrail, OrderQueryPlanner orderQueryPlanner) {
		this.orderQuestionGuardrail = orderQuestionGuardrail;
        this.orderQueryPlanner = orderQueryPlanner;
    }

	@Override
	public AskOrderResponse findAnswer(AskOrderRequest request) {
		OrderQueryFrame orderQueryFrame = validateQuestionIntent(request);
		orderQueryPlanner.plan(orderQueryFrame);
		return null;
	}

	private OrderQueryFrame validateQuestionIntent(AskOrderRequest request) {
		OrderQueryFrame frame = orderQuestionGuardrail.evaluate(request.question());
		String question = safeForLog(request.question());
		if (!frame.allowed()) {
			LOGGER.warn("ORDER_QUESTION status=REJECTED question=\"{}\"", question);
			throw new OrderQuestionRejectedException(frame.decision(), frame.reason());
		}
		LOGGER.info("ORDER_QUESTION status=ACCEPTED question=\"{}\"", question);
		return frame;
	}

	private static String safeForLog(String question) {
		return question.replaceAll("[\\r\\n\\t]", " ");
	}
}
