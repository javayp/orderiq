package com.orderiq.semantic;

import com.orderiq.data.model.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSemanticDocumentFactoryTest {

	private final OrderSemanticDocumentFactory factory = new OrderSemanticDocumentFactory();

	@Test
	void enrichesOrdersUsingDatasetRelativeValueAndDateBands() {
		List<Order> orders = new ArrayList<>();
		for (int index = 1; index <= 10; index++) {
			orders.add(order(index));
		}

		List<String> documents = factory.createDocuments(orders);

		assertThat(documents.get(0))
				.contains("very low value small tiny inexpensive")
				.contains("very old historical earliest");
		assertThat(documents.get(4))
				.contains("medium value standard")
				.contains("established date");
		assertThat(documents.get(9))
				.contains("very high value unusually expensive large premium")
				.contains("very recent latest newest")
				.contains("combined category very high value recent premium");
	}

	@Test
	void returnsNoDocumentsForAnEmptyDataset() {
		assertThat(factory.createDocuments(List.of())).isEmpty();
	}

	private static Order order(int index) {
		return new Order(
				Integer.toString(index),
				"C001",
				LocalDate.parse("2026-01-01").plusDays(index),
				BigDecimal.valueOf(index * 100L));
	}
}
