package com.orderiq.client.openai;

import com.orderiq.client.OrderQueryModelClient;
import com.orderiq.planning.OrderQueryPlan;
import com.orderiq.planning.OrderQueryPrompt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public final class SpringAiOrderQueryModelClient implements OrderQueryModelClient {

	private final ChatClient chatClient;

	public SpringAiOrderQueryModelClient(ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}

	@Override
	public OrderQueryPlan generate(OrderQueryPrompt prompt) {
		ResponseEntity<ChatResponse, OrderQueryPlan> response = chatClient.prompt()
				.system(prompt.systemMessage())
				.user(prompt.userMessage())
				.call()
				.responseEntity(
						OrderQueryPlan.class,
						ChatClient.EntityParamSpec::useProviderStructuredOutput);

		ChatResponse chatResponse = Objects.requireNonNull(
				response.getResponse(),
				"OpenAI response metadata must not be null");
		OrderQueryPlan plan = Objects.requireNonNull(
				response.entity(),
				"OpenAI structured query plan must not be null");

		log.info("ORDER_LLM token_usage={}", chatResponse.getMetadata().getUsage());
		return plan;
	}
}
