package com.orderiq.data.service.impl;

import com.orderiq.data.exception.IngestionException;
import com.orderiq.data.event.OrdersReloadedEvent;
import com.orderiq.data.event.OrdersReloadedPublisher;
import com.orderiq.data.model.IngestionIssue;
import com.orderiq.data.model.IngestionIssue.IssueCode;
import com.orderiq.data.model.IngestionReport;
import com.orderiq.data.model.NormalizationResult;
import com.orderiq.data.model.Order;
import com.orderiq.data.model.RawOrderRow;
import com.orderiq.data.repository.OrderStore;
import com.orderiq.data.service.OrderIngestionService;
import com.orderiq.data.service.OrderTransformer;
import com.orderiq.data.source.OrderSource;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public final class DefaultOrderIngestionService implements OrderIngestionService {

	private final OrderSource orderSource;
	private final OrderTransformer rowTransformer;
	private final OrderStore orderStore;
	private final OrdersReloadedPublisher eventPublisher;
	private final Clock clock;

	@Override
	public IngestionReport load(List<Path> sources) {
		if (sources == null || sources.isEmpty()) {
			throw new IngestionException("At least one CSV source is required");
		}
		if (sources.stream().anyMatch(Objects::isNull)) {
			throw new IngestionException("CSV sources must not contain null paths");
		}
		List<Path> normalizedSources = sources.stream()
				.map(Path::toAbsolutePath)
				.map(Path::normalize)
				.toList();

		IngestionAccumulator accumulator = new IngestionAccumulator();
		normalizedSources.stream()
				.map(orderSource::read)
				.flatMap(List::stream)
				.forEach(row -> accumulator.add(row, rowTransformer.transform(row)));

		List<Order> orders = accumulator.orders();
		orderStore.replaceAll(orders);
		Instant completedAt = clock.instant();
		eventPublisher.publish(new OrdersReloadedEvent(orders.size(), completedAt));

		return new IngestionReport(
				normalizedSources,
				accumulator.rowsRead,
				orders.size(),
				accumulator.rowsDropped,
				accumulator.amountsDefaulted,
				accumulator.currenciesDefaulted,
				completedAt,
				accumulator.issues);
	}

	private static final class IngestionAccumulator {
		private final Map<String, Order> ordersById = new LinkedHashMap<>();
		private final List<IngestionIssue> issues = new ArrayList<>();
		private int rowsRead;
		private int rowsDropped;
		private int amountsDefaulted;
		private int currenciesDefaulted;

		void add(RawOrderRow row, NormalizationResult result) {
			rowsRead++;
			issues.addAll(result.issues());
			amountsDefaulted += result.amountDefaulted() ? 1 : 0;
			currenciesDefaulted += result.currencyDefaulted() ? 1 : 0;
			if (result.order().isEmpty()) {
				rowsDropped++;
				return;
			}

			Order order = result.order().orElseThrow();
			if (ordersById.put(order.orderId(), order) != null) {
				rowsDropped++;
				issues.add(new IngestionIssue(row.rowNumber(), order.orderId(), IssueCode.DUPLICATE_ORDER_ID,
						"duplicate order_id replaced the earlier row"));
			}
		}

		List<Order> orders() {
			return List.copyOf(ordersById.values());
		}
	}
}
