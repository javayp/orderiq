package com.orderiq.api;

import com.orderiq.api.model.OrderResponse;
import com.orderiq.api.model.OrderStatsResponse;
import com.orderiq.data.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
final class OrderController {

	private final OrderQueryService queryService;

	@GetMapping("/customer/{customer_id}")
	List<OrderResponse> byCustomer(@PathVariable("customer_id") String customerId) {
		return queryService.forCustomer(customerId).stream().map(OrderResponse::from).toList();
	}

	@GetMapping("/stats")
	OrderStatsResponse statistics() {
		return OrderStatsResponse.from(queryService.statistics());
	}

	@GetMapping("/recent")
	List<OrderResponse> recent(@RequestParam("days") int days) {
		return queryService.recent(days).stream().map(OrderResponse::from).toList();
	}


}
