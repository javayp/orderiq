package com.orderiq.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

	@TempDir
	static Path tempDirectory;

	@DynamicPropertySource
	static void databasePath(DynamicPropertyRegistry registry) {
		registry.add("orderiq.database.path", () -> tempDirectory.resolve("orders.db").toString());
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Clock clock;

	private LocalDate today;

	@BeforeEach
	void loadOrders() {
		today = LocalDate.now(clock);
		jdbc.update("DELETE FROM orders");
		insert("RECENT-TODAY", "C001", today, "10.00");
		insert("RECENT-YESTERDAY", "C001", today.minusDays(1), "20.00");
		insert("OLD", "C002", today.minusDays(2), "5.00");
		insert("FUTURE", "C003", today.plusDays(1), "7.00");
	}

	@Test
	void returnsOrdersForOneCustomerInDateOrder() throws Exception {
		mockMvc.perform(get("/orders/customer/C001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].order_id").value("RECENT-YESTERDAY"))
				.andExpect(jsonPath("$[0].customer_id").value("C001"))
				.andExpect(jsonPath("$[0].order_date").value(today.minusDays(1).toString()))
				.andExpect(jsonPath("$[0].amount_usd").value(20.00))
				.andExpect(jsonPath("$[1].order_id").value("RECENT-TODAY"));
	}

	@Test
	void returnsTheRequiredStatisticsShape() throws Exception {
		mockMvc.perform(get("/orders/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total_revenue").value(42.00))
				.andExpect(jsonPath("$.avg_order_value").value(10.50))
				.andExpect(jsonPath("$.orders_per_day['" + today.minusDays(1) + "']").value(1))
				.andExpect(jsonPath("$.orders_per_day['" + today + "']").value(1));
	}

	@Test
	void recentWindowExcludesOlderAndFutureOrders() throws Exception {
		mockMvc.perform(get("/orders/recent").queryParam("days", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].order_id").value("RECENT-TODAY"))
				.andExpect(jsonPath("$[1].order_id").value("RECENT-YESTERDAY"))
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void rejectsInvalidRecentWindow() throws Exception {
		mockMvc.perform(get("/orders/recent").queryParam("days", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid order query"))
				.andExpect(jsonPath("$.detail").value("days must be greater than zero"));
	}

	@Test
	void reportsLiveness() throws Exception {
		mockMvc.perform(get("/healthz"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/plain"))
				.andExpect(content().string("ok"));
	}

	private void insert(String orderId, String customerId, LocalDate orderDate, String amountUsd) {
		jdbc.update("""
				INSERT INTO orders (order_id, customer_id, order_date, amount_usd)
				VALUES (?, ?, ?, ?)
				""", orderId, customerId, orderDate.toString(), amountUsd);
	}
}
