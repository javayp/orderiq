package com.orderiq.planning;

import com.orderiq.guardrail.OrderQueryFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.orderiq.guardrail.OrderQueryFrame.Decision.ALLOWED;
import static com.orderiq.guardrail.OrderQueryFrame.Decision.OUT_OF_DOMAIN;
import static com.orderiq.guardrail.OrderQueryFrame.Metric.TOTAL_REVENUE;
import static com.orderiq.guardrail.OrderQueryFrame.Sort.NONE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderQueryPromptFactoryTest {

	private final OrderQueryPromptFactory factory = new OrderQueryPromptFactory();

	@Test
	void createsSchemaBoundPromptFromAllowedFrame() {
		OrderQueryPrompt prompt = factory.create(allowedFrame());

		assertTrue(prompt.systemMessage().contains("order_id TEXT PRIMARY KEY"));
		assertTrue(prompt.systemMessage().contains("amount_usd NUMERIC NOT NULL"));
		assertTrue(prompt.systemMessage().contains("Return status QUERY with the generated SQL"));
		assertTrue(prompt.systemMessage().contains("Return status REJECTED with an empty SQL"));
		assertTrue(prompt.systemMessage().contains("Preserve every requested customer and order ID"));
		assertTrue(prompt.systemMessage().contains("use IN (...)"));
		assertTrue(prompt.userMessage().contains("total revenue for customers C001 and C002"));
		assertFalse(prompt.userMessage().contains("Trusted guardrail hints"));
	}

	@Test
	void appendsFailedSqlAndErrorToRetryPrompt() {
		OrderQueryPrompt prompt = factory.createRetry(
				allowedFrame(),
				"SELECT total FROM orders",
				"no such column: total");

		assertTrue(prompt.userMessage().contains("SELECT total FROM orders"));
		assertTrue(prompt.userMessage().contains("no such column: total"));
		assertTrue(prompt.userMessage().contains("without repeating the failed query"));
	}

	@Test
	void rejectsAFrameThatWasNotAllowedByTheGuardrail() {
		OrderQueryFrame rejected = new OrderQueryFrame(
				"Tell me a joke",
				"tell me a joke",
				OUT_OF_DOMAIN,
				Set.of(),
				Set.of(),
				NONE,
				List.of(),
				Optional.empty(),
				List.of(),
				"No supported order-domain concept was recognized.");

		assertThrows(IllegalArgumentException.class, () -> factory.create(rejected));
	}

	private static OrderQueryFrame allowedFrame() {
		return new OrderQueryFrame(
				"What is the total revenue for customers C001 and C002?",
				"what is the total revenue for customers c001 and c002?",
				ALLOWED,
				Set.of(TOTAL_REVENUE),
				Set.of(),
				NONE,
				List.of("C001", "C002"),
				Optional.empty(),
				List.of(),
				"");
	}
}
