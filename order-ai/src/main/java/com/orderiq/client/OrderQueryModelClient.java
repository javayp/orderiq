package com.orderiq.client;

import com.orderiq.planning.OrderQueryPlan;
import com.orderiq.planning.OrderQueryPrompt;

public interface OrderQueryModelClient {

	OrderQueryPlan generate(OrderQueryPrompt prompt);
}
