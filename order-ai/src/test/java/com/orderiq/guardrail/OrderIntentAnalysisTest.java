package com.orderiq.guardrail;

import com.orderiq.guardrail.OrderQueryFrame.Grouping;
import com.orderiq.guardrail.OrderQueryFrame.Metric;
import com.orderiq.guardrail.OrderQueryFrame.Decision;
import com.orderiq.guardrail.OrderQueryFrame.Sort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderIntentAnalysisTest {

	private final OrderQuestionGuardrail guardrail = OrderVocabularyTestFixture.guardrail();

	@Test
	void buildsAnExplainableFrameForTheExerciseQuestion() {
		OrderQueryFrame frame = guardrail.evaluate(
				"  What is the TOTAL   revenue for customer DK-13375 in the last 30 days?  ");

		assertEquals(Decision.ALLOWED, frame.decision());
		assertTrue(frame.metrics().contains(Metric.TOTAL_REVENUE));
		assertEquals(List.of("DK-13375"), frame.customerIds());
		assertEquals(
				"what is the total revenue for customer dk-13375 in the last 30 days?",
				frame.normalizedQuestion());
		assertTrue(frame.reason().isEmpty());
	}

	@Test
	void acceptsTheReadmeCustomerIdShape() {
		OrderQueryFrame frame = guardrail.evaluate("Show orders for customer C001");

		assertEquals(Decision.ALLOWED, frame.decision());
		assertEquals(List.of("C001"), frame.customerIds());
		assertTrue(frame.metrics().contains(Metric.ORDER_LIST));
	}

	@Test
	void preservesMixedCaseCustomerIdsFoundInTheDataset() {
		OrderQueryFrame frame = guardrail.evaluate("List orders for customer Co-12640");

		assertEquals(List.of("Co-12640"), frame.customerIds());
	}

	@Test
	void preservesMultipleCustomerIdsInQuestionOrderWithoutDuplicates() {
		OrderQueryFrame frame = guardrail.evaluate(
				"Compare customer C001 with client DK-13375 and customer c001");

		assertEquals(Decision.ALLOWED, frame.decision());
		assertEquals(List.of("C001", "DK-13375"), frame.customerIds());
	}

	@Test
	void capturesMoreThanOneRequestedMetric() {
		OrderQueryFrame frame = guardrail.evaluate(
				"Give me total revenue and number of orders for customer DK-13375");

		assertTrue(frame.metrics().contains(Metric.TOTAL_REVENUE));
		assertTrue(frame.metrics().contains(Metric.ORDER_COUNT));
	}

	@Test
	void recognizesMultipleGroupingDimensions() {
		OrderQueryFrame frame = guardrail.evaluate("Show daily sales by customer in 2024");

		assertTrue(frame.metrics().contains(Metric.TOTAL_REVENUE));
		assertTrue(frame.groupings().contains(Grouping.DAY));
		assertTrue(frame.groupings().contains(Grouping.CUSTOMER));
	}

	@Test
	void extractsADataShapedOrderId() {
		OrderQueryFrame frame = guardrail.evaluate("Show order 100006");

		assertEquals("100006", frame.orderId().orElseThrow());
		assertTrue(frame.metrics().contains(Metric.ORDER_LIST));
	}

	@Test
	void leavesDetailedAmountLanguageForTheLlm() {
		OrderQueryFrame frame = guardrail.evaluate("Show orders exceeding USD 500");

		assertEquals(Decision.ALLOWED, frame.decision());
		assertTrue(frame.metrics().contains(Metric.ORDER_LIST));
		assertEquals("Show orders exceeding USD 500", frame.originalQuestion());
	}

	@Test
	void recognizesConfiguredSortingHints() {
		OrderQueryFrame amount = guardrail.evaluate("Show top orders by amount");
		OrderQueryFrame date = guardrail.evaluate("List recent orders");

		assertEquals(Sort.AMOUNT_DESC, amount.sort());
		assertEquals(Sort.ORDER_DATE_DESC, date.sort());
	}

	@Test
	void recognizesTheBestCustomerIntent() {
		OrderQueryFrame frame = guardrail.evaluate("Which customer spent the most?");

		assertTrue(frame.metrics().contains(Metric.TOTAL_REVENUE));
		assertTrue(frame.groupings().contains(Grouping.CUSTOMER));
		assertEquals(Sort.AMOUNT_DESC, frame.sort());
	}

	@Test
	void rejectsOnlyClearlyUnsupportedSchemaConcepts() {
		OrderQueryFrame frame = guardrail.evaluate(
				"Which product category has the highest revenue by shipping region?");

		assertEquals(Decision.UNSUPPORTED_SCHEMA, frame.decision());
		assertTrue(frame.unsupportedTerms().contains("product"));
		assertTrue(frame.unsupportedTerms().contains("location"));
		assertTrue(frame.reason().contains("product"));
	}

	@Test
	void treatsOriginalCurrencyBreakdownsAsUnsupported() {
		OrderQueryFrame frame = guardrail.evaluate("Show total revenue by currency");

		assertEquals(Decision.UNSUPPORTED_SCHEMA, frame.decision());
		assertTrue(frame.unsupportedTerms().contains("original currency"));
	}

	@Test
	void leavesUnfamiliarNonDomainLanguageUnknown() {
		OrderQueryFrame frame = guardrail.evaluate("Will it rain tomorrow?");

		assertEquals(Decision.OUT_OF_DOMAIN, frame.decision());
		assertTrue(frame.metrics().isEmpty());
		assertFalse(frame.reason().isEmpty());
	}

	@Test
	void rejectsBlankQuestionsAtTheComponentBoundary() {
		assertThrows(IllegalArgumentException.class, () -> guardrail.evaluate("  "));
	}
}
