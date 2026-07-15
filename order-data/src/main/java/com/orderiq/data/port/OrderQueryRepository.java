package com.orderiq.data.port;

import com.orderiq.data.model.Order;
import com.orderiq.data.model.OrderStatistics;

import java.time.LocalDate;
import java.util.List;

public interface OrderQueryRepository {

	List<Order> findByCustomerId(String customerId);

	List<Order> findBetween(LocalDate fromInclusive, LocalDate toInclusive);

	OrderStatistics statistics();
}
