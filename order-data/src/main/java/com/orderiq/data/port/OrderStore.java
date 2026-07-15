package com.orderiq.data.port;

import com.orderiq.data.model.Order;

import java.util.List;

/** Replaces the durable normalized order dataset. */
public interface OrderStore {

	void replaceAll(List<Order> orders);
}
