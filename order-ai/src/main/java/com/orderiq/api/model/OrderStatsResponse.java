package com.orderiq.api.model;

import com.orderiq.data.model.OrderStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record OrderStatsResponse(
		BigDecimal totalRevenue,
		BigDecimal avgOrderValue,
		Map<LocalDate, Long> ordersPerDay) {

	public static OrderStatsResponse from(OrderStatistics statistics) {
		return new OrderStatsResponse(
				statistics.totalRevenue(),
				statistics.averageOrderValue(),
				statistics.ordersPerDay());
	}
}
