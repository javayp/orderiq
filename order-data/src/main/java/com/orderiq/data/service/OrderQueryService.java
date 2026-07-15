package com.orderiq.data.service;

import com.orderiq.data.model.Order;
import com.orderiq.data.model.OrderStatistics;

import java.util.List;

public interface OrderQueryService {

	List<Order> forCustomer(String customerId);

	OrderStatistics statistics();

	List<Order> recent(int days);
}
