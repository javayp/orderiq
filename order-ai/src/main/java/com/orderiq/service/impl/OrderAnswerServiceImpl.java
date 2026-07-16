package com.orderiq.service.impl;

import com.orderiq.api.model.AskOrderRequest;
import com.orderiq.api.model.AskOrderResponse;
import com.orderiq.data.exception.OrderSqlExecutionException;
import com.orderiq.data.repository.OrderSqlRepository;
import com.orderiq.exception.OrderQuestionRejectedException;
import com.orderiq.guardrail.OrderQueryFrame;
import com.orderiq.guardrail.OrderQuestionGuardrail;
import com.orderiq.planning.OrderQueryPlan;
import com.orderiq.planning.OrderQueryPlanValidator;
import com.orderiq.planning.OrderQueryPlanner;
import com.orderiq.planning.OrderSqlValidationException;
import com.orderiq.planning.OrderSqlValidator;
import com.orderiq.service.OrderAnswerService;
import com.orderiq.util.OrderAnswerFormatter;
import com.orderiq.util.TextSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public final class OrderAnswerServiceImpl implements OrderAnswerService {

	private final OrderQuestionGuardrail orderQuestionGuardrail;
	private final OrderQueryPlanner orderQueryPlanner;
	private final OrderSqlValidator orderSqlValidator;
	private final OrderSqlRepository orderSqlRepository;

	@Override
	public AskOrderResponse findAnswer(AskOrderRequest request) {
		// Convert the accepted question into a structured order-query frame.
		OrderQueryFrame frame = validateQuestionIntent(request);

		// Ask the model to generate the initial SQL plan.
		OrderQueryPlan initialPlan = orderQueryPlanner.plan(frame);

		// Stop before database access if the model rejected the request.
		OrderQueryPlanValidator.requireExecutable(initialPlan);

		// Validate and execute the SQL, allowing one model correction on failure.
		QueryExecution execution = executeWithOneRetry(frame, initialPlan);

		// Turn the database rows into a short user-facing summary.
		String answer = OrderAnswerFormatter.format(execution.rows());

		// Return the summary, the SQL that succeeded, and the complete result rows.
		return new AskOrderResponse(
				answer,
				execution.plan().sql(),
				execution.rows());
	}

	private QueryExecution executeWithOneRetry(OrderQueryFrame frame, OrderQueryPlan initialPlan) {
		try {
			// Use the initial plan when its SQL is valid and SQLite can execute it.
			return execute(initialPlan);
		} catch (OrderSqlValidationException | OrderSqlExecutionException exception) {
			// Keep the retry error single-line and bounded before placing it in a prompt.
			String failureReason = TextSupport.boundedSingleLine(
					exception.getMessage(),
					500,
					"SQL validation or execution failed");

			// Record why the first attempt failed before requesting a correction.
			log.warn("ORDER_SQL status=RETRY error=\"{}\"", failureReason);

			// Send the original question, failed SQL, and error back to the model.
			OrderQueryPlan correctedPlan = orderQueryPlanner.retry(
					frame,
					initialPlan.sql(),
					failureReason);

			// A retry must still produce an executable query plan.
			OrderQueryPlanValidator.requireExecutable(correctedPlan);

			// Execute once more; a second failure is returned without another retry.
			return execute(correctedPlan);
		}
	}

	private QueryExecution execute(OrderQueryPlan plan) {
		// Apply read-only and single-statement checks before database access.
		orderSqlValidator.validate(plan.sql());

		// Submit the validated SQL to SQLite and capture its dynamic result columns.
		List<Map<String, Object>> rows = orderSqlRepository.executeQuery(plan.sql());

		// Keep the successful plan with its rows so sql_used is always accurate.
		return new QueryExecution(plan, rows);
	}

	private OrderQueryFrame validateQuestionIntent(AskOrderRequest request) {
		// Evaluate the question with deterministic guardrails before calling the model.
		OrderQueryFrame frame = orderQuestionGuardrail.evaluate(request.question());

		// Remove control characters before including user input in application logs.
		String question = TextSupport.singleLine(request.question());

		// Reject questions that are unsafe, unsupported, or outside the order domain.
		if (!frame.allowed()) {
			log.warn("ORDER_QUESTION status=REJECTED question=\"{}\"", question);
			throw new OrderQuestionRejectedException(frame.decision(), frame.reason());
		}

		// Record accepted questions so the complete request flow is observable.
		log.info("ORDER_QUESTION status=ACCEPTED question=\"{}\"", question);

		// Pass the extracted order intent to query planning.
		return frame;
	}

	private record QueryExecution(OrderQueryPlan plan, List<Map<String, Object>> rows) {
	}
}
