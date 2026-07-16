package com.orderiq.service;

import com.orderiq.guardrail.OrderQueryFrame;
import com.orderiq.guardrail.OrderQuestionGuardrail;
import com.orderiq.api.model.AskOrderRequest;
import com.orderiq.api.model.AskOrderResponse;
import com.orderiq.exception.OrderQuestionRejectedException;
import com.orderiq.planning.OrderQueryPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public final class OrderAnswerServiceImpl implements OrderAnswerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderAnswerServiceImpl.class);

	private final OrderQuestionGuardrail orderQuestionGuardrail;
	private final OrderQueryPlan orderQueryPlan;

	public OrderAnswerServiceImpl(OrderQuestionGuardrail orderQuestionGuardrail, OrderQueryPlan orderQueryPlan) {
		this.orderQuestionGuardrail = orderQuestionGuardrail;
        this.orderQueryPlan = orderQueryPlan;
    }

	@Override
	public AskOrderResponse findAnswer(AskOrderRequest request) {
		OrderQueryFrame orderQueryFrame = validateQuestionIntent(request);
		orderQueryPlan.plan(orderQueryFrame);
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
