package com.orderiq.data.service.impl;

import com.orderiq.data.model.IngestionIssue;
import com.orderiq.data.model.IngestionIssue.IssueCode;
import com.orderiq.data.model.NormalizationResult;
import com.orderiq.data.model.Order;
import com.orderiq.data.model.RawOrderRow;
import com.orderiq.data.policy.CurrencyConverter;
import com.orderiq.data.policy.OrderDateParser;
import com.orderiq.data.service.OrderTransformer;
import com.orderiq.data.util.TextValues;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public final class DefaultOrderTransformer implements OrderTransformer {

	private final OrderDateParser dateParser;
	private final CurrencyConverter currencyConverter;

	@Override
	public NormalizationResult transform(RawOrderRow row) {
		String orderId = TextValues.trimToNull(row.orderId());
		if (orderId == null) {
			return NormalizationResult.dropped(issue(row, null, IssueCode.MISSING_ORDER_ID,
					"order_id is required"));
		}

		String customerId = TextValues.trimToNull(row.customerId());
		if (customerId == null) {
			return NormalizationResult.dropped(issue(row, orderId, IssueCode.MISSING_CUSTOMER_ID,
					"customer_id is required"));
		}

		Optional<LocalDate> orderDate = dateParser.parse(row.orderDate());
		if (orderDate.isEmpty()) {
			return NormalizationResult.dropped(issue(row, orderId, IssueCode.INVALID_ORDER_DATE,
					"order_date must match a supported calendar-date format"));
		}

		List<IngestionIssue> issues = new ArrayList<>();
		boolean amountDefaulted = false;
		BigDecimal amount;
		String rawAmount = TextValues.trimToNull(row.amount());
		if (rawAmount == null) {
			amount = BigDecimal.ZERO;
			amountDefaulted = true;
			issues.add(issue(row, orderId, IssueCode.MISSING_AMOUNT_DEFAULTED,
					"amount was missing and defaulted to 0"));
		} else {
			try {
				amount = new BigDecimal(rawAmount);
			} catch (NumberFormatException exception) {
				amount = BigDecimal.ZERO;
				amountDefaulted = true;
				issues.add(issue(row, orderId, IssueCode.INVALID_AMOUNT_DEFAULTED,
						"amount was invalid and defaulted to 0"));
			}
		}

		boolean currencyDefaulted = false;
		String currency = TextValues.trimToNull(row.currency());
		if (currency == null) {
			currency = "USD";
			currencyDefaulted = true;
			issues.add(issue(row, orderId, IssueCode.MISSING_CURRENCY_DEFAULTED,
					"currency was missing and defaulted to USD"));
		} else {
			currency = currency.toUpperCase(Locale.ROOT);
		}

		Optional<BigDecimal> amountUsd = currencyConverter.convertToUsd(amount, currency);
		if (amountUsd.isEmpty()) {
			return NormalizationResult.dropped(issue(row, orderId, IssueCode.UNSUPPORTED_CURRENCY,
					"currency is not supported"));
		}

		Order order = new Order(orderId, customerId, orderDate.orElseThrow(),
				amountUsd.orElseThrow().setScale(2, RoundingMode.HALF_UP));
		return new NormalizationResult(Optional.of(order), amountDefaulted, currencyDefaulted, issues);
	}

	private static IngestionIssue issue(RawOrderRow row, String orderId, IssueCode code, String detail) {
		return new IngestionIssue(row.rowNumber(), orderId, code, detail);
	}
}
