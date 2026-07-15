package com.orderiq.data.policy.impl;

import com.orderiq.data.policy.CurrencyConverter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FixedRateCurrencyConverter implements CurrencyConverter {

	private final Map<String, BigDecimal> usdRates;

	public FixedRateCurrencyConverter(Map<String, BigDecimal> usdRates) {
		Objects.requireNonNull(usdRates, "usdRates must not be null");
		Map<String, BigDecimal> normalizedRates = new LinkedHashMap<>();
		usdRates.forEach((currency, rate) -> {
			if (currency == null || currency.isBlank()) {
				throw new IllegalArgumentException("currency code must not be blank");
			}
			if (rate == null || rate.signum() <= 0) {
				throw new IllegalArgumentException("currency rate must be greater than zero");
			}
			normalizedRates.put(currency.toUpperCase(Locale.ROOT), rate);
		});
		this.usdRates = Map.copyOf(normalizedRates);
	}

	@Override
	public Optional<BigDecimal> convertToUsd(BigDecimal amount, String currency) {
		Objects.requireNonNull(amount, "amount must not be null");
		if (currency == null) {
			return Optional.empty();
		}
		BigDecimal rate = usdRates.get(currency.toUpperCase(Locale.ROOT));
		return rate == null ? Optional.empty() : Optional.of(amount.multiply(rate));
	}
}
