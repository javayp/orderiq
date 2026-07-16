package com.orderiq.planning;

import com.orderiq.client.OrderQueryModelClient;
import com.orderiq.guardrail.OrderQueryFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderQueryPlanner {

    private final OrderQueryPromptFactory orderQueryPromptFactory;
    private final OrderQueryModelClient orderQueryModelClient;

    @Autowired
    public OrderQueryPlanner(OrderQueryPromptFactory orderQueryPromptFactory, OrderQueryModelClient orderQueryModelClient) {
        this.orderQueryPromptFactory = orderQueryPromptFactory;
        this.orderQueryModelClient = orderQueryModelClient;
    }

    public void plan(OrderQueryFrame orderQueryFrame) {
        OrderQueryPrompt orderQueryPrompt = orderQueryPromptFactory.create(orderQueryFrame);
        log.info("OrderQueryPrompt {}",orderQueryPrompt);
        OrderQueryPlan orderQueryPlan = orderQueryModelClient.generate(orderQueryPrompt);
        log.info("OrderQueryPlan {}",orderQueryPlan);
    }
}
