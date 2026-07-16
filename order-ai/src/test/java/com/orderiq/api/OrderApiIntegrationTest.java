package com.orderiq.api;

import com.orderiq.client.OrderQueryModelClient;
import com.orderiq.data.model.Order;
import com.orderiq.planning.OrderQueryPlan;
import com.orderiq.semantic.InMemoryOrderSemanticIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static com.orderiq.planning.OrderQueryPlan.Status.QUERY;
import static com.orderiq.planning.OrderQueryPlan.Status.REJECTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

	@MockitoBean
	OrderQueryModelClient orderQueryModelClient;

	@MockitoBean
	EmbeddingModel embeddingModel;

	@Autowired
	InMemoryOrderSemanticIndex semanticIndex;

	private LocalDate today;

	@BeforeEach
	void loadOrders() {
		today = LocalDate.now(clock);
		jdbc.update("DELETE FROM orders");
		insert("RECENT-TODAY", "C001", today, "10.00");
		insert("RECENT-YESTERDAY", "C001", today.minusDays(1), "20.00");
		insert("OLD", "C002", today.minusDays(2), "5.00");
		insert("FUTURE", "C003", today.plusDays(1), "7.00");
		semanticIndex.replace(
				List.of(
						order("RECENT-TODAY", "C001", today, "10.00"),
						order("RECENT-YESTERDAY", "C001", today.minusDays(1), "20.00"),
						order("OLD", "C002", today.minusDays(2), "5.00"),
						order("FUTURE", "C003", today.plusDays(1), "7.00")),
				List.of(
						new float[]{1, 0},
						new float[]{0.8f, 0.2f},
						new float[]{0, 1},
						new float[]{-1, 0}));
		when(embeddingModel.embed(anyString())).thenReturn(new float[]{1, 0});
		when(orderQueryModelClient.generate(any())).thenReturn(new OrderQueryPlan(
				QUERY,
				"SELECT COUNT(*) AS total_orders FROM orders",
				""));
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
				.andExpect(jsonPath("$.orders_per_day['%s']".formatted(today.minusDays(1))).value(1))
				.andExpect(jsonPath("$.orders_per_day['%s']".formatted(today)).value(1));
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

	@Test
	void reportsReadinessAfterTheSemanticIndexIsAvailable() throws Exception {
		mockMvc.perform(get("/readyz"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/plain"))
				.andExpect(content().string("ready"));
	}

	@Test
	void returnsTheTopSemanticOrderMatches() throws Exception {
		mockMvc.perform(get("/orders/semantic_search")
					.queryParam("q", "recent customer orders")
					.queryParam("top_k", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].order_id").value("RECENT-TODAY"))
				.andExpect(jsonPath("$[0].score").value(1.0))
				.andExpect(jsonPath("$[1].order_id").value("RECENT-YESTERDAY"))
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void rejectsInvalidSemanticSearchParameters() throws Exception {
		mockMvc.perform(get("/orders/semantic_search")
					.queryParam("q", " ")
					.queryParam("top_k", "5"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("q must not be blank"));

		mockMvc.perform(get("/orders/semantic_search")
					.queryParam("q", "recent orders")
					.queryParam("top_k", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("top_k must be between 1 and 50"));

		mockMvc.perform(get("/orders/semantic_search").queryParam("top_k", "5"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("q is required"));

		mockMvc.perform(get("/orders/semantic_search")
					.queryParam("q", "orders")
					.queryParam("top_k", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("top_k must be an integer"));
	}

	@Test
	void rejectsOutOfDomainTextBeforeSemanticEmbedding() throws Exception {
		mockMvc.perform(get("/orders/semantic_search")
					.queryParam("q", "tell me a joke")
					.queryParam("top_k", "2"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Order question rejected"))
				.andExpect(jsonPath("$.decision").value("OUT_OF_DOMAIN"));

		verify(embeddingModel, never()).embed("tell me a joke");
	}

	@Test
	void acceptsAnOrderQuestionForTheLlmPipeline() throws Exception {
		mockMvc.perform(post("/orders/ask")
					.contentType("application/json")
					.content("""
							{"question":"Give me order statistics"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.answer").value("Total orders: 4"))
				.andExpect(jsonPath("$.sql_used").value("SELECT COUNT(*) AS total_orders FROM orders"))
				.andExpect(jsonPath("$.rows[0].total_orders").value(4));
	}

	@Test
	void retriesOneFailedSqlStatementAndReturnsTheCorrectedResult() throws Exception {
		String correctedSql = "SELECT ROUND(SUM(amount_usd), 2) AS total_revenue FROM orders";
		when(orderQueryModelClient.generate(any())).thenReturn(
				new OrderQueryPlan(QUERY, "SELECT total FROM orders", ""),
				new OrderQueryPlan(QUERY, correctedSql, ""));

		mockMvc.perform(post("/orders/ask")
					.contentType("application/json")
					.content("""
							{"question":"What is the total revenue?"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.answer").value("Total revenue: 42.0"))
				.andExpect(jsonPath("$.sql_used").value(correctedSql))
				.andExpect(jsonPath("$.rows[0].total_revenue").value(42.0));

		verify(orderQueryModelClient, times(2)).generate(any());
	}

	@Test
	void returnsBadRequestWhenTheModelRejectsTheAvailableSchema() throws Exception {
		when(orderQueryModelClient.generate(any())).thenReturn(new OrderQueryPlan(
				REJECTED,
				"",
				"Product categories are not available in the orders schema."));

		mockMvc.perform(post("/orders/ask")
					.contentType("application/json")
					.content("""
							{"question":"Show me all orders"}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid order query"))
				.andExpect(jsonPath("$.detail")
						.value("Product categories are not available in the orders schema."));
	}

	@Test
	void returnsBadRequestForUnsupportedSchemaBeforeCallingTheModel() throws Exception {
		mockMvc.perform(post("/orders/ask")
					.contentType("application/json")
					.content("""
							{"question":"Show revenue by product category"}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Order question rejected"))
				.andExpect(jsonPath("$.decision").value("UNSUPPORTED_SCHEMA"));

		verify(orderQueryModelClient, never()).generate(any());
	}

	@Test
	void stopsAfterTheCorrectedSqlAlsoFails() throws Exception {
		when(orderQueryModelClient.generate(any())).thenReturn(
				new OrderQueryPlan(QUERY, "DELETE FROM orders", ""),
				new OrderQueryPlan(QUERY, "DROP TABLE orders", ""));

		mockMvc.perform(post("/orders/ask")
					.contentType("application/json")
					.content("""
							{"question":"Show me all orders"}
							"""))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.title").value("Order query execution failed"))
				.andExpect(jsonPath("$.detail")
						.value("Unable to generate an executable order query after one retry."));

		verify(orderQueryModelClient, times(2)).generate(any());
	}

	@Test
	void rejectsAnOutOfDomainQuestionBeforeTheLlmPipeline() throws Exception {
		mockMvc.perform(post("/orders/ask")
					.contentType("application/json")
					.content("""
							{"question":"Who are you?"}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Order question rejected"))
				.andExpect(jsonPath("$.decision").value("OUT_OF_DOMAIN"));
	}

	private void insert(String orderId, String customerId, LocalDate orderDate, String amountUsd) {
		jdbc.update("""
				INSERT INTO orders (order_id, customer_id, order_date, amount_usd)
				VALUES (?, ?, ?, ?)
				""", orderId, customerId, orderDate.toString(), amountUsd);
	}

	private static Order order(String orderId, String customerId, LocalDate orderDate, String amountUsd) {
		return new Order(orderId, customerId, orderDate, new BigDecimal(amountUsd));
	}
}
