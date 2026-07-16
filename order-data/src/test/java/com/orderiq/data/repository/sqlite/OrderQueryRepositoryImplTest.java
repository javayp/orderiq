package com.orderiq.data.repository.sqlite;

import com.orderiq.data.model.Order;
import com.orderiq.data.model.OrderStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class OrderQueryRepositoryImplTest {

	@TempDir
	Path tempDirectory;

	private OrderQueryRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		SQLiteDataSource dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:%s".formatted(tempDirectory.resolve("orders.db")));
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("""
				CREATE TABLE orders (
				    order_id TEXT PRIMARY KEY,
				    customer_id TEXT NOT NULL,
				    order_date TEXT NOT NULL,
				    amount_usd NUMERIC NOT NULL
				)
				""");
		jdbc.execute("""
				CREATE TABLE order_dataset_state (
				    state_id INTEGER PRIMARY KEY,
				    revision INTEGER NOT NULL
				)
				""");
		jdbc.update("INSERT INTO order_dataset_state (state_id, revision) VALUES (1, 0)");
		new OrderStoreImpl(jdbc).replaceAll(List.of(
				order("1001", "C001", "2026-07-14", "10.00"),
				order("1002", "C001", "2026-07-13", "11.00"),
				order("1003", "C002", "2026-07-13", "5.50"),
				order("1004", "C' OR 1=1 --", "2026-07-15", "2.00")));
		repository = new OrderQueryRepositoryImpl(jdbc);
	}

	@Test
	void returnsAllOrdersAndTheDatasetRevision() {
		assertThat(repository.findAll())
				.extracting(Order::orderId)
				.containsExactly("1001", "1002", "1003", "1004");
		assertThat(repository.datasetRevision()).isEqualTo(1L);
	}

	@Test
	void queriesCustomersWithParametersAndDeterministicOrdering() {
		assertThat(repository.findByCustomerId("C001"))
				.extracting(Order::orderId)
				.containsExactly("1002", "1001");
		assertThat(repository.findByCustomerId("C' OR 1=1 --"))
				.extracting(Order::orderId)
				.containsExactly("1004");
	}

	@Test
	void returnsOnlyTheRequestedDateWindow() {
		assertThat(repository.findBetween(
				LocalDate.parse("2026-07-13"), LocalDate.parse("2026-07-14")))
				.extracting(Order::orderId)
				.containsExactly("1001", "1002", "1003");
	}

	@Test
	void calculatesRevenueAverageAndDailyCounts() {
		OrderStatistics statistics = repository.statistics();

		assertThat(statistics.totalRevenue()).isEqualByComparingTo("28.50");
		assertThat(statistics.averageOrderValue()).isEqualByComparingTo("7.13");
		assertThat(statistics.ordersPerDay())
				.containsExactly(
						entry(LocalDate.parse("2026-07-13"), 2L),
						entry(LocalDate.parse("2026-07-14"), 1L),
						entry(LocalDate.parse("2026-07-15"), 1L));
	}

	private static Order order(String orderId, String customerId, String date, String amount) {
		return new Order(orderId, customerId, LocalDate.parse(date), new BigDecimal(amount));
	}
}
