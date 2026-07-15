package com.orderiq.data.adapter.sqlite;

import com.orderiq.data.model.Order;
import com.orderiq.data.model.OrderStatistics;
import com.orderiq.data.port.OrderQueryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class SqliteOrderQueryRepository implements OrderQueryRepository {

	private static final RowMapper<Order> ORDER_ROW_MAPPER = (resultSet, rowNumber) -> new Order(
			resultSet.getString("order_id"),
			resultSet.getString("customer_id"),
			LocalDate.parse(resultSet.getString("order_date")),
			resultSet.getBigDecimal("amount_usd").setScale(2, RoundingMode.HALF_UP));

	private static final String SELECT_COLUMNS = "order_id, customer_id, order_date, amount_usd";
	private static final String REVENUE_AGGREGATE_SQL = """
			SELECT COALESCE(ROUND(SUM(amount_usd), 2), 0) AS total_revenue,
			       COUNT(*) AS order_count
			FROM orders
			""";
	private static final String DAILY_ORDER_COUNTS_SQL = """
			SELECT order_date, COUNT(*) AS order_count
			FROM orders
			GROUP BY order_date
			ORDER BY order_date ASC
			""";

	private final JdbcTemplate jdbcTemplate;

	public SqliteOrderQueryRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<Order> findByCustomerId(String customerId) {
		return jdbcTemplate.query("""
				SELECT %s
				FROM orders
				WHERE customer_id = ?
				ORDER BY order_date ASC, order_id ASC
				""".formatted(SELECT_COLUMNS), ORDER_ROW_MAPPER, customerId);
	}

	@Override
	public List<Order> findBetween(LocalDate fromInclusive, LocalDate toInclusive) {
		return jdbcTemplate.query("""
				SELECT %s
				FROM orders
				WHERE order_date BETWEEN ? AND ?
				ORDER BY order_date DESC, order_id ASC
				""".formatted(SELECT_COLUMNS), ORDER_ROW_MAPPER,
				fromInclusive.toString(), toInclusive.toString());
	}

	@Override
	public OrderStatistics statistics() {
		RevenueAggregate revenue = queryRevenueAggregate();
		Map<LocalDate, Long> ordersPerDay = queryOrdersPerDay();
		BigDecimal averageOrderValue = calculateAverageOrderValue(revenue);

		return new OrderStatistics(revenue.totalRevenue(), averageOrderValue, ordersPerDay);
	}

	private RevenueAggregate queryRevenueAggregate() {
		return Objects.requireNonNull(jdbcTemplate.queryForObject(
				REVENUE_AGGREGATE_SQL,
				(resultSet, rowNumber) -> new RevenueAggregate(
						new BigDecimal(resultSet.getString("total_revenue"))
								.setScale(2, RoundingMode.HALF_UP),
						resultSet.getLong("order_count"))));
	}

	private Map<LocalDate, Long> queryOrdersPerDay() {
		List<DailyOrderCount> dailyCounts = jdbcTemplate.query(
				DAILY_ORDER_COUNTS_SQL,
				(resultSet, rowNumber) -> new DailyOrderCount(
						LocalDate.parse(resultSet.getString("order_date")),
						resultSet.getLong("order_count")));

		Map<LocalDate, Long> ordersPerDay = new LinkedHashMap<>();
		for (DailyOrderCount dailyCount : dailyCounts) {
			ordersPerDay.put(dailyCount.orderDate(), dailyCount.orderCount());
		}
		return ordersPerDay;
	}

	private static BigDecimal calculateAverageOrderValue(RevenueAggregate revenue) {
		return revenue.orderCount() == 0
				? BigDecimal.ZERO.setScale(2)
				: revenue.totalRevenue().divide(
						BigDecimal.valueOf(revenue.orderCount()), 2, RoundingMode.HALF_UP);
	}

	private record RevenueAggregate(BigDecimal totalRevenue, long orderCount) {
	}

	private record DailyOrderCount(LocalDate orderDate, long orderCount) {
	}
}
