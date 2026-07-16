package com.orderiq.semantic;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orderiq.semantic")
public record SemanticSearchProperties(
		boolean enabled,
		int batchSize,
		int maxTopK,
		int maxQueryLength,
		double minimumScore,
		long refreshIntervalMs) {

	public SemanticSearchProperties {
		if (batchSize < 1 || maxTopK < 1 || maxQueryLength < 1 || refreshIntervalMs < 1) {
			throw new IllegalArgumentException("Semantic search limits must be greater than zero");
		}
		if (minimumScore < 0 || minimumScore > 1) {
			throw new IllegalArgumentException("Semantic search minimum score must be between zero and one");
		}
	}
}
