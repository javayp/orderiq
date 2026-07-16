package com.orderiq.data.repository.sqlite;

import com.orderiq.data.model.Order;
import com.orderiq.data.repository.OrderStore;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderStoreImpl implements OrderStore {

	private static final String INSERT_SQL = """
			INSERT INTO orders (order_id, customer_id, order_date, amount_usd)
			VALUES (?, ?, ?, ?)
			""";

	private final JdbcTemplate jdbcTemplate;

	@Override
	@Transactional
	public void replaceAll(List<Order> orders) {
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.batchUpdate(INSERT_SQL, orders, 500, (PreparedStatement statement, Order order) -> {
			statement.setString(1, order.orderId());
			statement.setString(2, order.customerId());
			statement.setString(3, order.orderDate().toString());
			statement.setBigDecimal(4, order.amountUsd());
		});
	}
}
