package com.orderiq.service.impl;

import com.orderiq.data.exception.InvalidOrderQueryException;
import com.orderiq.exception.OrderQuestionRejectedException;
import com.orderiq.guardrail.OrderQueryFrame;
import com.orderiq.guardrail.OrderQuestionGuardrail;
import com.orderiq.semantic.InMemoryOrderSemanticIndex;
import com.orderiq.semantic.SemanticOrderMatch;
import com.orderiq.semantic.SemanticSearchProperties;
import com.orderiq.semantic.SemanticSearchUnavailableException;
import com.orderiq.service.OrderSemanticSearchService;
import com.orderiq.util.TextSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "orderiq.semantic", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public final class OrderSemanticSearchServiceImpl implements OrderSemanticSearchService {

	private final EmbeddingModel embeddingModel;
	private final InMemoryOrderSemanticIndex semanticIndex;
	private final SemanticSearchProperties properties;
	private final OrderQuestionGuardrail orderQuestionGuardrail;

	@Override
	public List<SemanticOrderMatch> search(String query, int topK) {
		String normalizedQuery = validateQuery(query);
		validateTopK(topK);
		validateOrderDomain(normalizedQuery);

		try {
			float[] queryVector = embeddingModel.embed(normalizedQuery);
			List<SemanticOrderMatch> nearestMatches = semanticIndex.search(queryVector, topK);
			return retainRelevantMatches(nearestMatches);
		} catch (SemanticSearchUnavailableException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new SemanticSearchUnavailableException(
					"Unable to embed the semantic search query", exception);
		}
	}

	private void validateOrderDomain(String query) {
		OrderQueryFrame frame = orderQuestionGuardrail.evaluate(query);
		boolean supportedSemanticFragment = frame.decision() == OrderQueryFrame.Decision.OUT_OF_DOMAIN
				&& orderQuestionGuardrail.hasSupportedOrderEvidence(query);
		if (!frame.allowed() && !supportedSemanticFragment) {
			throw new OrderQuestionRejectedException(frame.decision(), frame.reason());
		}
	}

	private List<SemanticOrderMatch> retainRelevantMatches(List<SemanticOrderMatch> nearestMatches) {
		List<SemanticOrderMatch> relevantMatches = new ArrayList<>(nearestMatches.size());
		for (SemanticOrderMatch match : nearestMatches) {
			if (match.score() >= properties.minimumScore()) {
				relevantMatches.add(match);
			}
		}
		return List.copyOf(relevantMatches);
	}

	private String validateQuery(String query) {
		if (query == null || query.isBlank()) {
			throw new InvalidOrderQueryException("q must not be blank");
		}
		String normalized = TextSupport.singleLine(query).trim();
		if (normalized.length() > properties.maxQueryLength()) {
			throw new InvalidOrderQueryException(
					"q must not exceed %d characters".formatted(properties.maxQueryLength()));
		}
		return normalized;
	}

	private void validateTopK(int topK) {
		if (topK < 1 || topK > properties.maxTopK()) {
			throw new InvalidOrderQueryException(
					"top_k must be between 1 and %d".formatted(properties.maxTopK()));
		}
	}
}
