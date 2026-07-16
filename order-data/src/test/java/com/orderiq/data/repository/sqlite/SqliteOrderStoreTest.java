package com.orderiq.data.repository.sqlite;

import com.orderiq.data.model.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteOrderStoreTest {

	@TempDir
	Path tempDirectory;

	@Test
	void replacesOrdersAndStoresDatesAsIsoText() {
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
		SqliteOrderStore store = new SqliteOrderStore(jdbc);

		store.replaceAll(List.of(new Order(
				"1001", "C001", LocalDate.parse("2024-03-15"), new BigDecimal("11.00"))));

		Map<String, Object> row = jdbc.queryForMap("""
				SELECT order_date, typeof(order_date) AS date_type, amount_usd
				FROM orders WHERE order_id = '1001'
				""");
		assertThat(row.get("order_date")).isEqualTo("2024-03-15");
		assertThat(row.get("date_type")).isEqualTo("text");
		assertThat(new BigDecimal(row.get("amount_usd").toString())).isEqualByComparingTo("11.00");
	}
}
