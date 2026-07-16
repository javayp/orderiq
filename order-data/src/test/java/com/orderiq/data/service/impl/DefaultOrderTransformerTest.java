package com.orderiq.data.service.impl;

import com.orderiq.data.model.NormalizationResult;
import com.orderiq.data.model.Order;
import com.orderiq.data.model.RawOrderRow;
import com.orderiq.data.policy.CurrencyConverter;
import com.orderiq.data.policy.OrderDateParser;
import com.orderiq.data.policy.impl.FixedRateCurrencyConverter;
import com.orderiq.data.service.OrderTransformer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOrderTransformerTest {

	@Test
	void acceptsNewDateAndCurrencyPoliciesWithoutChangingTheTransformer() {
		OrderDateParser dottedDateParser = value -> Optional.of(
				LocalDate.parse(value, DateTimeFormatter.ofPattern("dd.MM.uuuu")));
		CurrencyConverter converter = new FixedRateCurrencyConverter(Map.of(
				"GBP", new BigDecimal("1.25")));
		OrderTransformer transformer = new DefaultOrderTransformer(dottedDateParser, converter);

		NormalizationResult result = transformer.transform(new RawOrderRow(
				2, "1001", "C001", "15.01.2024", "10", "GBP"));

		assertThat(result.order()).hasValueSatisfying(order -> assertThat(order)
				.extracting(Order::orderId, Order::customerId, Order::orderDate, Order::amountUsd)
				.containsExactly("1001", "C001", LocalDate.parse("2024-01-15"), new BigDecimal("12.50")));
	}
}
