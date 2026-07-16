package com.orderiq.semantic;

import com.orderiq.data.model.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
public final class InMemoryOrderSemanticIndex {

	private static final Comparator<SemanticOrderMatch> BEST_MATCH_FIRST =
			Comparator.comparingDouble(SemanticOrderMatch::score)
					.reversed()
					.thenComparing(match -> match.order().orderId());

	private final AtomicReference<List<IndexedOrder>> snapshot = new AtomicReference<>();

	public void replace(List<Order> orders, List<float[]> vectors) {
		Objects.requireNonNull(orders, "orders must not be null");
		Objects.requireNonNull(vectors, "vectors must not be null");
		if (orders.size() != vectors.size()) {
			throw new IllegalArgumentException("Each order must have exactly one embedding");
		}

		List<IndexedOrder> replacement = new ArrayList<>(orders.size());
		for (int index = 0; index < orders.size(); index++) {
			replacement.add(new IndexedOrder(orders.get(index), vectors.get(index)));
		}
		snapshot.set(List.copyOf(replacement));
	}

	public List<SemanticOrderMatch> search(float[] queryVector, int topK) {
		List<IndexedOrder> indexedOrders = snapshot.get();
		if (indexedOrders == null) {
			throw new SemanticSearchUnavailableException("The semantic order index is not ready");
		}
		if (indexedOrders.isEmpty()) {
			return List.of();
		}

		double queryNorm = vectorNorm(queryVector);
		if (queryNorm == 0) {
			throw new SemanticSearchUnavailableException("The semantic query produced an empty vector");
		}

		List<SemanticOrderMatch> matches = new ArrayList<>(indexedOrders.size());
		for (IndexedOrder indexedOrder : indexedOrders) {
			double score = cosineSimilarity(queryVector, queryNorm, indexedOrder);
			matches.add(new SemanticOrderMatch(indexedOrder.order(), score));
		}
		matches.sort(BEST_MATCH_FIRST);

		int resultSize = Math.min(topK, matches.size());
		return List.copyOf(matches.subList(0, resultSize));
	}

	public boolean isReady() {
		return snapshot.get() != null;
	}

	public int size() {
		List<IndexedOrder> indexedOrders = snapshot.get();
		return indexedOrders == null ? 0 : indexedOrders.size();
	}

	private static double cosineSimilarity(
			float[] queryVector,
			double queryNorm,
			IndexedOrder indexedOrder) {
		float[] orderVector = indexedOrder.vector();
		if (queryVector.length != orderVector.length) {
			throw new SemanticSearchUnavailableException("Embedding dimensions do not match");
		}

		double dotProduct = 0;
		for (int index = 0; index < queryVector.length; index++) {
			dotProduct += queryVector[index] * orderVector[index];
		}
		return dotProduct / (queryNorm * indexedOrder.norm());
	}

	private static double vectorNorm(float[] vector) {
		Objects.requireNonNull(vector, "embedding vector must not be null");
		double squaredValues = 0;
		for (float value : vector) {
			squaredValues += value * value;
		}
		return Math.sqrt(squaredValues);
	}

	private record IndexedOrder(Order order, float[] vector, double norm) {

		private IndexedOrder(Order order, float[] vector) {
			this(
					Objects.requireNonNull(order, "order must not be null"),
					Arrays.copyOf(vector, vector.length),
					vectorNorm(vector));
			if (norm == 0) {
				throw new IllegalArgumentException("Order embeddings must not be empty vectors");
			}
		}
	}
}
