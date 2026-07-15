package com.orderiq.data.service.impl;

import com.orderiq.data.adapter.csv.CsvOrderReader;
import com.orderiq.data.model.IngestionReport;
import com.orderiq.data.model.Order;
import com.orderiq.data.model.OrdersReloadedEvent;
import com.orderiq.data.policy.impl.FixedRateCurrencyConverter;
import com.orderiq.data.policy.impl.FlexibleOrderDateParser;
import com.orderiq.data.port.OrderStore;
import com.orderiq.data.service.OrderIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOrderIngestionServiceTest {

	@TempDir
	Path tempDirectory;

	@Test
	void appliesTheRequiredCleaningAndConversionRules() throws Exception {
		Path csv = tempDirectory.resolve("orders.csv");
		Files.writeString(csv, """
				order_id,customer_id,order_date,amount,currency
				1001,C001,2024-01-02,10,EUR
				,C002,2024-01-02,20,USD
				1002,C002,01/03/2024,,
				1003,C003,31-02-2024,50,USD
				1004,C004,2024/01/04,N/A,USD
				""");

		CapturingOrderStore store = new CapturingOrderStore();
		List<OrdersReloadedEvent> events = new ArrayList<>();
		Clock clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC);
		OrderIngestionService service = new DefaultOrderIngestionService(
				new CsvOrderReader(),
				new DefaultOrderTransformer(
						new FlexibleOrderDateParser(),
						new FixedRateCurrencyConverter(Map.of(
								"USD", BigDecimal.ONE,
								"EUR", new BigDecimal("1.10")))),
				store,
				events::add,
				clock);

		IngestionReport report = service.load(List.of(csv));

		assertThat(report.rowsRead()).isEqualTo(5);
		assertThat(report.rowsLoaded()).isEqualTo(3);
		assertThat(report.rowsDropped()).isEqualTo(2);
		assertThat(report.amountsDefaulted()).isEqualTo(2);
		assertThat(report.currenciesDefaulted()).isEqualTo(1);
		assertThat(store.orders)
				.extracting(Order::orderId, Order::customerId, order -> order.orderDate().toString(),
						order -> order.amountUsd().toPlainString())
				.containsExactly(
						tuple("1001", "C001", "2024-01-02", "11.00"),
						tuple("1002", "C002", "2024-01-03", "0.00"),
						tuple("1004", "C004", "2024-01-04", "0.00"));
		assertThat(events).containsExactly(new OrdersReloadedEvent(3, clock.instant()));
	}

	private static org.assertj.core.groups.Tuple tuple(Object... values) {
		return org.assertj.core.groups.Tuple.tuple(values);
	}

	private static final class CapturingOrderStore implements OrderStore {
		private List<Order> orders = List.of();

		@Override
		public void replaceAll(List<Order> orders) {
			this.orders = List.copyOf(orders);
		}
	}
}
