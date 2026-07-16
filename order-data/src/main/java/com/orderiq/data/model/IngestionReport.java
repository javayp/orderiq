package com.orderiq.data.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Immutable summary returned by an ETL load. */
public record IngestionReport(
		List<Path> sources,
		int rowsRead,
		int rowsLoaded,
		int rowsDropped,
		int amountsDefaulted,
		int currenciesDefaulted,
		Instant completedAt,
		List<IngestionIssue> issues) {

	public IngestionReport {
		sources = List.copyOf(sources);
		issues = List.copyOf(issues);
	}
}
