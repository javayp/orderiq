package com.orderiq.data.repository;

import com.orderiq.data.model.Order;

import java.util.List;

/** Replaces the durable normalized order dataset. */
public interface OrderStore {

	void replaceAll(List<Order> orders);
}
