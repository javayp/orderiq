package com.orderiq.guardrail;

import com.orderiq.guardrail.OrderQueryFrame.Metric;
import com.orderiq.guardrail.OrderQueryFrame.Decision;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A broad corpus generated from the supplied CSV's real identifier and amount
 * shapes. This complements the smaller, curated behavior tests.
 */
class OrderQuestionCorpusTest {

	private static final Pattern NUMERIC_AMOUNT = Pattern.compile("\\d+(?:\\.\\d+)?");
	private static final OrderQuestionGuardrail GUARDRAIL = OrderVocabularyTestFixture.guardrail();

	@ParameterizedTest(name = "customer corpus [{index}] {0}")
	@MethodSource("customerQuestions")
	void understandsCustomerQuestionsBuiltFromSuppliedData(
			String question,
			String customerId,
			Metric expectedMetric) {
		OrderQueryFrame frame = GUARDRAIL.evaluate(question);

		assertTrue(frame.allowed());
		assertEquals(Decision.ALLOWED, frame.decision());
		assertEquals(List.of(customerId), frame.customerIds());
		assertTrue(frame.metrics().contains(expectedMetric));
	}

	@ParameterizedTest(name = "order corpus [{index}] {0}")
	@MethodSource("orderQuestions")
	void understandsOrderIdsBuiltFromSuppliedData(String question, String orderId) {
		OrderQueryFrame frame = GUARDRAIL.evaluate(question);

		assertTrue(frame.allowed());
		assertEquals(Decision.ALLOWED, frame.decision());
		assertEquals(orderId, frame.orderId().orElseThrow());
		assertTrue(frame.metrics().contains(Metric.ORDER_LIST));
	}

	@ParameterizedTest(name = "amount corpus [{index}] {0}")
	@MethodSource("amountQuestions")
	void keepsDataShapedAmountQuestionsAvailableForTheLlm(String question, String amount) {
		OrderQueryFrame frame = GUARDRAIL.evaluate(question);

		assertTrue(frame.allowed());
		assertEquals(Decision.ALLOWED, frame.decision());
		assertTrue(frame.metrics().contains(Metric.ORDER_LIST));
		assertTrue(frame.originalQuestion().contains(amount));
	}

	private static Stream<Arguments> customerQuestions() throws IOException {
		List<String> customerIds = distinctColumn(1, value -> !value.isBlank()).stream()
				.limit(120)
				.toList();
		List<CustomerTemplate> templates = List.of(
				new CustomerTemplate("Show all orders for customer %s", Metric.ORDER_LIST),
				new CustomerTemplate("What is the total revenue for customer %s?", Metric.TOTAL_REVENUE),
				new CustomerTemplate("How many orders does customer %s have?", Metric.ORDER_COUNT),
				new CustomerTemplate("Average order value for client %s", Metric.AVERAGE_ORDER_VALUE),
				new CustomerTemplate("List the latest 5 orders for customer id %s", Metric.ORDER_LIST),
				new CustomerTemplate("Daily sales for customer %s in 2024", Metric.TOTAL_REVENUE));

		return customerIds.stream().flatMap(customerId -> templates.stream()
				.map(template -> Arguments.of(
						template.question().formatted(customerId),
						customerId,
						template.metric())));
	}

	private static Stream<Arguments> orderQuestions() throws IOException {
		List<String> orderIds = distinctColumn(0, value -> value.matches("\\d{3,20}"))
				.stream()
				.limit(150)
				.toList();
		List<String> templates = List.of(
				"Show order %s",
				"Find order id %s",
				"Give me order number %s");

		List<Arguments> questions = new ArrayList<>();
		for (int index = 0; index < orderIds.size(); index++) {
			String orderId = orderIds.get(index);
			String template = templates.get(index % templates.size());
			questions.add(Arguments.of(template.formatted(orderId), orderId));
		}
		return questions.stream();
	}

	private static Stream<Arguments> amountQuestions() throws IOException {
		return distinctColumn(3, value -> NUMERIC_AMOUNT.matcher(value).matches()).stream()
				.limit(100)
				.map(amount -> Arguments.of("Show orders over $%s".formatted(amount), amount));
	}

	private static List<String> distinctColumn(int column, Predicate<String> filter) throws IOException {
		Set<String> values = new LinkedHashSet<>();
		for (String[] row : rows()) {
			if (row.length <= column) {
				continue;
			}
			String value = row[column].strip();
			if (filter.test(value)) {
				values.add(value);
			}
		}
		return List.copyOf(values);
	}

	private static List<String[]> rows() throws IOException {
		try (Stream<String> lines = Files.lines(datasetPath())) {
			return lines.skip(1)
					.map(line -> line.split(",", -1))
					.toList();
		}
	}

	private static Path datasetPath() {
		List<Path> candidates = List.of(
				Path.of("data", "orders.csv"),
				Path.of("..", "data", "orders.csv"));
		return candidates.stream()
				.filter(Files::isRegularFile)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Unable to locate data/orders.csv"));
	}

	private record CustomerTemplate(String question, Metric metric) {
	}
}
