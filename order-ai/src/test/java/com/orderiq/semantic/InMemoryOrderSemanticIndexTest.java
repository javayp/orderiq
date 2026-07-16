package com.orderiq.semantic;

import com.orderiq.data.model.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryOrderSemanticIndexTest {

	private final InMemoryOrderSemanticIndex index = new InMemoryOrderSemanticIndex();

	@Test
	void returnsTheClosestOrdersByCosineSimilarity() {
		index.replace(
				List.of(order("A"), order("B"), order("C")),
				List.of(
						new float[]{1, 0},
						new float[]{0.8f, 0.2f},
						new float[]{0, 1}));

		List<SemanticOrderMatch> matches = index.search(new float[]{1, 0}, 2);

		assertThat(matches).extracting(match -> match.order().orderId())
				.containsExactly("A", "B");
		assertThat(matches.getFirst().score()).isEqualTo(1.0);
	}

	@Test
	void atomicallyReplacesThePreviousSnapshot() {
		index.replace(List.of(order("A")), List.of(new float[]{1, 0}));
		index.replace(List.of(order("B")), List.of(new float[]{0, 1}));

		assertThat(index.search(new float[]{0, 1}, 5))
				.extracting(match -> match.order().orderId())
				.containsExactly("B");
		assertThat(index.size()).isEqualTo(1);
	}

	@Test
	void rejectsSearchBeforeTheFirstSnapshotIsReady() {
		assertThatThrownBy(() -> index.search(new float[]{1, 0}, 1))
				.isInstanceOf(SemanticSearchUnavailableException.class)
				.hasMessageContaining("not ready");
	}

	private static Order order(String orderId) {
		return new Order(
				orderId,
				"C001",
				LocalDate.parse("2026-07-16"),
				new BigDecimal("10.00"));
	}
}
