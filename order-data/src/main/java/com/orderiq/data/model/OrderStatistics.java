package com.orderiq.data.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record OrderStatistics(
		BigDecimal totalRevenue,
		BigDecimal averageOrderValue,
		Map<LocalDate, Long> ordersPerDay) {

	public OrderStatistics {
		Objects.requireNonNull(totalRevenue, "totalRevenue must not be null");
		Objects.requireNonNull(averageOrderValue, "averageOrderValue must not be null");
		Objects.requireNonNull(ordersPerDay, "ordersPerDay must not be null");
		ordersPerDay = Collections.unmodifiableMap(new LinkedHashMap<>(ordersPerDay));
	}
}
