package com.orderiq.semantic;

import com.orderiq.data.event.OrdersReloadedEvent;
import com.orderiq.data.model.Order;
import com.orderiq.data.repository.OrderQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "orderiq.semantic", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public final class OrderSemanticIndexManager {

	private final OrderQueryRepository orderRepository;
	private final EmbeddingModel embeddingModel;
	private final InMemoryOrderSemanticIndex semanticIndex;
	private final OrderSemanticDocumentFactory documentFactory;
	private final SemanticSearchProperties properties;

	private long indexedRevision = -1;

	@EventListener(ApplicationReadyEvent.class)
	public void initialize() {
		refreshSafely();
	}

	@Scheduled(fixedDelayString = "${orderiq.semantic.refresh-interval-ms:5000}")
	public void refreshOnSchedule() {
		refreshSafely();
	}

	@EventListener(OrdersReloadedEvent.class)
	public void refreshAfterInProcessReload() {
		refreshSafely();
	}

	public synchronized void refreshIfNeeded() {
		long currentRevision = orderRepository.datasetRevision();
		if (semanticIndex.isReady() && currentRevision == indexedRevision) {
			return;
		}

		List<Order> orders = orderRepository.findAll();
		List<String> documents = documentFactory.createDocuments(orders);
		List<float[]> vectors = embedInBatches(documents);
		semanticIndex.replace(orders, vectors);
		indexedRevision = currentRevision;
		log.info("ORDER_SEMANTIC_INDEX status=READY revision={} orders={}",
				indexedRevision, semanticIndex.size());
	}

	private void refreshSafely() {
		try {
			refreshIfNeeded();
		} catch (RuntimeException exception) {
			log.error("ORDER_SEMANTIC_INDEX status=FAILED error=\"{}\"", exception.getMessage());
		}
	}

	private List<float[]> embedInBatches(List<String> documents) {
		List<float[]> vectors = new ArrayList<>(documents.size());
		for (int start = 0; start < documents.size(); start += properties.batchSize()) {
			int end = Math.min(start + properties.batchSize(), documents.size());
			vectors.addAll(embeddingModel.embed(documents.subList(start, end)));
		}
		return vectors;
	}

}
