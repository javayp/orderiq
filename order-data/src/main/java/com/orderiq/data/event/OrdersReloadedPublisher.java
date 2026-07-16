package com.orderiq.data.event;

@FunctionalInterface
public interface OrdersReloadedPublisher {

	void publish(OrdersReloadedEvent event);
}
