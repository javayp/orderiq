package com.orderiq.service;

import com.orderiq.guardrail.OrderQueryFrame;
import com.orderiq.guardrail.OrderQuestionGuardrail;
import com.orderiq.api.model.AskOrderRequest;
import com.orderiq.api.model.AskOrderResponse;
import com.orderiq.exception.OrderQuestionRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class OrderAnswerServiceImpl implements OrderAnswerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderAnswerServiceImpl.class);
	private static final String ACCEPTED_MESSAGE = "Question accepted for LLM processing.";

	private final OrderQuestionGuardrail orderQuestionGuardrail;

	public OrderAnswerServiceImpl(OrderQuestionGuardrail orderQuestionGuardrail) {
		this.orderQuestionGuardrail = orderQuestionGuardrail;
	}

	@Override
	public AskOrderResponse findAnswer(AskOrderRequest request) {
		OrderQueryFrame frame = orderQuestionGuardrail.evaluate(request.question());
		String question = safeForLog(request.question());

		if (!frame.allowed()) {
			LOGGER.warn("ORDER_QUESTION status=REJECTED question=\"{}\"", question);
			throw new OrderQuestionRejectedException(frame.decision(), frame.reason());
		}

		LOGGER.info("ORDER_QUESTION status=ACCEPTED question=\"{}\"", question);
		return new AskOrderResponse(ACCEPTED_MESSAGE, null, List.of());
	}

	private static String safeForLog(String question) {
		return question.replaceAll("[\\r\\n\\t]", " ");
	}
}
