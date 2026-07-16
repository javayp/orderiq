package com.orderiq.planning;

import com.orderiq.guardrail.OrderQueryFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderQueryPlan {

    private OrderQueryPromptFactory orderQueryPromptFactory;

    public OrderQueryPlan(OrderQueryPromptFactory orderQueryPromptFactory) {
        this.orderQueryPromptFactory = orderQueryPromptFactory;
    }

    public void plan(OrderQueryFrame orderQueryFrame) {
        OrderQueryPrompt orderQueryPrompt = orderQueryPromptFactory.create(orderQueryFrame);
        log.info("OrderQueryPrompt {}",orderQueryPrompt);
    }
}
