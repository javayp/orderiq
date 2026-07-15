package com.orderiq.data.model;

import java.util.List;
import java.util.Optional;

public record NormalizationResult(
		Optional<Order> order,
		boolean amountDefaulted,
		boolean currencyDefaulted,
		List<IngestionIssue> issues) {

	public static NormalizationResult dropped(IngestionIssue issue) {
		return new NormalizationResult(Optional.empty(), false, false, List.of(issue));
	}
}
