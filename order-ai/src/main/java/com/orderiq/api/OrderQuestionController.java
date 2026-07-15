package com.orderiq.api;

import com.orderiq.api.model.AskOrderRequest;
import com.orderiq.api.model.AskOrderResponse;
import com.orderiq.service.OrderAnswerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderQuestionController {

	private final OrderAnswerService orderAnswerService;

	public OrderQuestionController(OrderAnswerService orderAnswerService) {
		this.orderAnswerService = orderAnswerService;
	}

	@PostMapping("/ask")
	public AskOrderResponse ask(@RequestBody @Valid AskOrderRequest request) {
		return orderAnswerService.findAnswer(request);
	}
}
