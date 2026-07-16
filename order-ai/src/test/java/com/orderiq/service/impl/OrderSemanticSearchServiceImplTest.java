package com.orderiq.service.impl;

import com.orderiq.data.model.Order;
import com.orderiq.exception.OrderQuestionRejectedException;
import com.orderiq.guardrail.OrderQueryFrame;
import com.orderiq.guardrail.OrderQuestionGuardrail;
import com.orderiq.semantic.InMemoryOrderSemanticIndex;
import com.orderiq.semantic.SemanticOrderMatch;
import com.orderiq.semantic.SemanticSearchProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderSemanticSearchServiceImplTest {

	@Test
	void removesNearestMatchesBelowTheConfiguredScore() {
		EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
		InMemoryOrderSemanticIndex index = new InMemoryOrderSemanticIndex();
		SemanticSearchProperties properties = new SemanticSearchProperties(
				true, 128, 50, 500, 0.20, 5000);
		OrderQuestionGuardrail guardrail = allowedGuardrail();
		OrderSemanticSearchServiceImpl service = new OrderSemanticSearchServiceImpl(
				embeddingModel, index, properties, guardrail);
		index.replace(List.of(order("A")), List.of(new float[]{1, 0}));
		when(embeddingModel.embed("unrelated text")).thenReturn(new float[]{0, 1});

		List<SemanticOrderMatch> matches = service.search("unrelated text", 5);

		assertThat(matches).isEmpty();
	}

	@Test
	void rejectsAnOutOfDomainQueryBeforeGeneratingAnEmbedding() {
		EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
		OrderQuestionGuardrail guardrail = mock(OrderQuestionGuardrail.class);
		OrderQueryFrame frame = mock(OrderQueryFrame.class);
		SemanticSearchProperties properties = new SemanticSearchProperties(
				true, 128, 50, 500, 0.20, 5000);
		OrderSemanticSearchServiceImpl service = new OrderSemanticSearchServiceImpl(
				embeddingModel, new InMemoryOrderSemanticIndex(), properties, guardrail);
		when(guardrail.evaluate("tell me a joke")).thenReturn(frame);
		when(frame.allowed()).thenReturn(false);
		when(frame.decision()).thenReturn(OrderQueryFrame.Decision.OUT_OF_DOMAIN);
		when(frame.reason()).thenReturn("No supported order-domain concept was recognized.");

		assertThatThrownBy(() -> service.search("tell me a joke", 5))
				.isInstanceOf(OrderQuestionRejectedException.class)
				.hasMessageContaining("No supported order-domain concept");
		verifyNoInteractions(embeddingModel);
	}

	private static OrderQuestionGuardrail allowedGuardrail() {
		OrderQuestionGuardrail guardrail = mock(OrderQuestionGuardrail.class);
		OrderQueryFrame frame = mock(OrderQueryFrame.class);
		when(guardrail.evaluate("unrelated text")).thenReturn(frame);
		when(frame.allowed()).thenReturn(true);
		return guardrail;
	}

	private static Order order(String orderId) {
		return new Order(
				orderId,
				"C001",
				LocalDate.parse("2026-07-16"),
				new BigDecimal("10.00"));
	}
}
