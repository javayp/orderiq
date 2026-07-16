package com.orderiq.data.model;

/** A non-fatal problem found while ingesting a CSV row. */
public record IngestionIssue(long rowNumber, String orderId, IssueCode code, String detail) {

	public enum IssueCode {
		MISSING_ORDER_ID,
		MISSING_CUSTOMER_ID,
		INVALID_ORDER_DATE,
		INVALID_AMOUNT_DEFAULTED,
		MISSING_AMOUNT_DEFAULTED,
		MISSING_CURRENCY_DEFAULTED,
		UNSUPPORTED_CURRENCY,
		DUPLICATE_ORDER_ID
	}
}
