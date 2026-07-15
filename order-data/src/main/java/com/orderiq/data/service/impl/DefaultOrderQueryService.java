package com.orderiq.data.service.impl;

import com.orderiq.data.exception.InvalidOrderQueryException;
import com.orderiq.data.model.Order;
import com.orderiq.data.model.OrderStatistics;
import com.orderiq.data.port.OrderQueryRepository;
import com.orderiq.data.service.OrderQueryService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class DefaultOrderQueryService implements OrderQueryService {

	private static final int MAX_CUSTOMER_ID_LENGTH = 100;

	private final OrderQueryRepository repository;
	private final Clock clock;

	public DefaultOrderQueryService(OrderQueryRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Override
	public List<Order> forCustomer(String customerId) {
		String normalizedCustomerId = normalizeCustomerId(customerId);
		return repository.findByCustomerId(normalizedCustomerId);
	}

	@Override
	public OrderStatistics statistics() {
		return repository.statistics();
	}

	@Override
	public List<Order> recent(int days) {
		if (days < 1) {
			throw new InvalidOrderQueryException("days must be greater than zero");
		}
		LocalDate today = LocalDate.now(clock);
		LocalDate fromInclusive = today.minusDays(days - 1L);
		return repository.findBetween(fromInclusive, today);
	}

	private static String normalizeCustomerId(String customerId) {
		if (customerId == null || customerId.isBlank()) {
			throw new InvalidOrderQueryException("customer_id must not be blank");
		}
		String normalized = customerId.trim();
		if (normalized.length() > MAX_CUSTOMER_ID_LENGTH) {
			throw new InvalidOrderQueryException(
					"customer_id must not exceed " + MAX_CUSTOMER_ID_LENGTH + " characters");
		}
		return normalized;
	}
}
