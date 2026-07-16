package com.orderiq.planning;

import com.orderiq.util.TextSupport;

/** Provider-independent system and user messages for one SQL planning request. */
public record OrderQueryPrompt(String systemMessage, String userMessage) {

	public OrderQueryPrompt {
		systemMessage = TextSupport.requireText(systemMessage, "systemMessage");
		userMessage = TextSupport.requireText(userMessage, "userMessage");
	}
}
