package com.orderiq.data.policy;

import java.math.BigDecimal;
import java.util.Optional;

public interface CurrencyConverter {

	Optional<BigDecimal> convertToUsd(BigDecimal amount, String currency);
}
