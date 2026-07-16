package com.orderiq.client.openai;

import com.orderiq.client.OrderQueryModelClient;
import com.orderiq.planning.OrderQueryPlan;
import com.orderiq.planning.OrderQueryPrompt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringAiOrderQueryModelClient implements OrderQueryModelClient {

    private final ChatClient chatClient;

    public SpringAiOrderQueryModelClient(ChatClient.Builder builder){
        this.chatClient=builder.build();
    }

    @Override
    public OrderQueryPlan generate(OrderQueryPrompt prompt) {
        ResponseEntity<ChatResponse, OrderQueryPlan> chatResponseOrderQueryPlanResponseEntity = chatClient.prompt()
                .system(prompt.systemMessage())
                .user(prompt.userMessage())
                .call()
                .responseEntity(
                        OrderQueryPlan.class,
                        ChatClient.EntityParamSpec::useProviderStructuredOutput);
        assert chatResponseOrderQueryPlanResponseEntity.getResponse() != null;
        log.info("Token usage {}",chatResponseOrderQueryPlanResponseEntity.getResponse().getMetadata().getUsage());
        return chatResponseOrderQueryPlanResponseEntity.entity();
    }
}
