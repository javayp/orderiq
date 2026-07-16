package com.orderiq.service.impl;

import com.orderiq.api.model.AskOrderRequest;
import com.orderiq.api.model.AskOrderResponse;
import com.orderiq.data.exception.OrderSqlExecutionException;
import com.orderiq.data.repository.OrderSqlRepository;
import com.orderiq.guardrail.OrderQueryFrame;
import com.orderiq.guardrail.OrderQuestionGuardrail;
import com.orderiq.planning.OrderQueryPlan;
import com.orderiq.planning.OrderQueryPlanner;
import com.orderiq.planning.OrderSqlValidationException;
import com.orderiq.planning.OrderSqlValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.orderiq.guardrail.OrderQueryFrame.Decision.ALLOWED;
import static com.orderiq.guardrail.OrderQueryFrame.Sort.NONE;
import static com.orderiq.planning.OrderQueryPlan.Status.QUERY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderAnswerServiceImplTest {

	private final OrderQuestionGuardrail guardrail = mock(OrderQuestionGuardrail.class);
	private final OrderQueryPlanner planner = mock(OrderQueryPlanner.class);
	private final OrderSqlRepository repository = mock(OrderSqlRepository.class);
	private final OrderAnswerServiceImpl service = new OrderAnswerServiceImpl(
			guardrail,
			planner,
			new OrderSqlValidator(),
			repository);

	@Test
	void executesTheInitialGeneratedSql() {
		OrderQueryFrame frame = allowedFrame();
		OrderQueryPlan plan = new OrderQueryPlan(QUERY, "SELECT COUNT(*) AS total_orders FROM orders", "");
		List<Map<String, Object>> rows = List.of(Map.of("total_orders", 3));
		when(guardrail.evaluate(frame.originalQuestion())).thenReturn(frame);
		when(planner.plan(frame)).thenReturn(plan);
		when(repository.executeQuery(plan.sql())).thenReturn(rows);

		AskOrderResponse response = service.findAnswer(new AskOrderRequest(frame.originalQuestion()));

		assertThat(response.sqlUsed()).isEqualTo(plan.sql());
		assertThat(response.rows()).isEqualTo(rows);
		assertThat(response.answer()).isEqualTo("Total orders: 3");
		verify(planner, never()).retry(frame, plan.sql(), "");
	}

	@Test
	void describesEveryColumnFromASingleResultRow() {
		OrderQueryFrame frame = allowedFrame();
		OrderQueryPlan plan = new OrderQueryPlan(
				QUERY,
				"SELECT SUM(amount_usd) AS total_revenue, COUNT(*) AS order_count FROM orders",
				"");
		Map<String, Object> aggregate = new LinkedHashMap<>();
		aggregate.put("total_revenue", 35.0);
		aggregate.put("order_count", 3);
		when(guardrail.evaluate(frame.originalQuestion())).thenReturn(frame);
		when(planner.plan(frame)).thenReturn(plan);
		when(repository.executeQuery(plan.sql())).thenReturn(List.of(aggregate));

		AskOrderResponse response = service.findAnswer(new AskOrderRequest(frame.originalQuestion()));

		assertThat(response.answer()).isEqualTo("Total revenue: 35.0, Order count: 3");
	}

	@Test
	void retriesOnceAfterSqliteExecutionFailure() {
		OrderQueryFrame frame = allowedFrame();
		OrderQueryPlan failed = new OrderQueryPlan(QUERY, "SELECT total FROM orders", "");
		OrderQueryPlan corrected = new OrderQueryPlan(
				QUERY,
				"SELECT SUM(amount_usd) AS total_revenue FROM orders",
				"");
		OrderSqlExecutionException failure = new OrderSqlExecutionException(
				"no such column: total",
				new IllegalStateException("database failure"));
		when(guardrail.evaluate(frame.originalQuestion())).thenReturn(frame);
		when(planner.plan(frame)).thenReturn(failed);
		when(repository.executeQuery(failed.sql())).thenThrow(failure);
		when(planner.retry(frame, failed.sql(), failure.getMessage())).thenReturn(corrected);
		when(repository.executeQuery(corrected.sql())).thenReturn(List.of(Map.of("total_revenue", 35)));

		AskOrderResponse response = service.findAnswer(new AskOrderRequest(frame.originalQuestion()));

		assertThat(response.sqlUsed()).isEqualTo(corrected.sql());
		verify(planner).retry(frame, failed.sql(), "no such column: total");
	}

	@Test
	void doesNotRetryAfterTheCorrectedSqlAlsoFails() {
		OrderQueryFrame frame = allowedFrame();
		OrderQueryPlan failed = new OrderQueryPlan(QUERY, "DELETE FROM orders", "");
		OrderQueryPlan corrected = new OrderQueryPlan(QUERY, "DROP TABLE orders", "");
		when(guardrail.evaluate(frame.originalQuestion())).thenReturn(frame);
		when(planner.plan(frame)).thenReturn(failed);
		when(planner.retry(frame, failed.sql(), "Only SELECT queries are allowed")).thenReturn(corrected);

		assertThatThrownBy(() -> service.findAnswer(new AskOrderRequest(frame.originalQuestion())))
				.isInstanceOf(OrderSqlValidationException.class);

		verify(planner).retry(frame, failed.sql(), "Only SELECT queries are allowed");
		verify(repository, never()).executeQuery(failed.sql());
		verify(repository, never()).executeQuery(corrected.sql());
	}

	private static OrderQueryFrame allowedFrame() {
		return new OrderQueryFrame(
				"How many orders are there?",
				"how many orders are there?",
				ALLOWED,
				Set.of(OrderQueryFrame.Metric.ORDER_COUNT),
				Set.of(),
				NONE,
				List.of(),
				Optional.empty(),
				List.of(),
				"");
	}
}
