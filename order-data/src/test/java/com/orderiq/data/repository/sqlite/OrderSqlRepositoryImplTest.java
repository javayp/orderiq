package com.orderiq.data.repository.sqlite;

import com.orderiq.data.exception.OrderSqlExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderSqlRepositoryImplTest {

	@TempDir
	Path tempDirectory;

	private OrderSqlRepositoryImpl repository;

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
		jdbc.update("""
				INSERT INTO orders (order_id, customer_id, order_date, amount_usd)
				VALUES ('1001', 'C001', '2026-07-15', 25.50),
				       ('1002', 'C001', '2026-07-16', 10.00),
				       ('1003', 'C002', '2026-07-16', 5.00)
				""");
		repository = new OrderSqlRepositoryImpl(jdbc);
	}

	@Test
	void executesDynamicSelectAndReturnsRows() {
		List<Map<String, Object>> rows = repository.executeQuery("""
				SELECT customer_id, COUNT(*) AS total_orders
				FROM orders
				GROUP BY customer_id
				ORDER BY customer_id
				""");

		assertThat(rows).hasSize(2);
		assertThat(rows.getFirst())
				.containsEntry("customer_id", "C001")
				.containsEntry("total_orders", 2);
	}

	@Test
	void convertsSqliteFailureIntoApplicationException() {
		assertThatThrownBy(() -> repository.executeQuery("SELECT missing_column FROM orders"))
				.isInstanceOf(OrderSqlExecutionException.class)
				.hasMessageContaining("missing_column");
	}
}
