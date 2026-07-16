package com.orderiq.semantic;

import com.orderiq.data.model.Order;
import com.orderiq.data.repository.OrderQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderSemanticIndexManagerTest {

	@Test
	void rebuildsInBatchesAndSkipsAnUnchangedRevision() {
		OrderQueryRepository repository = mock(OrderQueryRepository.class);
		EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
		InMemoryOrderSemanticIndex index = new InMemoryOrderSemanticIndex();
		SemanticSearchProperties properties = new SemanticSearchProperties(true, 2, 10, 200, 0.20, 5000);
		List<Order> orders = List.of(order("A"), order("B"), order("C"));

		when(repository.datasetRevision()).thenReturn(7L);
		when(repository.findAll()).thenReturn(orders);
		when(embeddingModel.embed(anyList())).thenAnswer(invocation -> {
			List<String> documents = invocation.getArgument(0);
			return documents.stream().map(ignored -> new float[]{1, 0}).toList();
		});
		OrderSemanticIndexManager manager = new OrderSemanticIndexManager(
				repository, embeddingModel, index, new OrderSemanticDocumentFactory(), properties);

		manager.refreshIfNeeded();
		manager.refreshIfNeeded();

		assertThat(index.size()).isEqualTo(3);
		verify(repository, times(1)).findAll();
		verify(embeddingModel, times(2)).embed(anyList());
	}

	private static Order order(String orderId) {
		return new Order(
				orderId,
				"C001",
				LocalDate.parse("2026-07-16"),
				new BigDecimal("10.00"));
	}
}
