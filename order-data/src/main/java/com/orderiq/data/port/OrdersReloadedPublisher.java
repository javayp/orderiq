package com.orderiq.data.port;

import com.orderiq.data.model.OrdersReloadedEvent;

@FunctionalInterface
public interface OrdersReloadedPublisher {

	void publish(OrdersReloadedEvent event);
}
