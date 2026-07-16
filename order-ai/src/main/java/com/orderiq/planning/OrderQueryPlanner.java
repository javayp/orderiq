package com.orderiq.planning;

import com.orderiq.client.OrderQueryModelClient;
import com.orderiq.guardrail.OrderQueryFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderQueryPlanner {

	private final OrderQueryPromptFactory orderQueryPromptFactory;
	private final OrderQueryModelClient orderQueryModelClient;

	public OrderQueryPlan plan(OrderQueryFrame orderQueryFrame) {
		return generate(orderQueryPromptFactory.create(orderQueryFrame));
	}

	public OrderQueryPlan retry(
			OrderQueryFrame orderQueryFrame,
			String failedSql,
			String errorMessage) {
		return generate(orderQueryPromptFactory.createRetry(orderQueryFrame, failedSql, errorMessage));
	}

	private OrderQueryPlan generate(OrderQueryPrompt prompt) {
		log.info("OrderQueryPrompt {}", prompt);
		OrderQueryPlan plan = orderQueryModelClient.generate(prompt);
		log.info("OrderQueryPlan {}", plan);
		return plan;
	}
}
