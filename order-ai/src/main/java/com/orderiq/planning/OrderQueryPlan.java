package com.orderiq.planning;

public record OrderQueryPlan(Status status, String sql, String reason) {

	public enum Status {
		QUERY,
		REJECTED
	}
}
