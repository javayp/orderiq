package com.orderiq.data.policy;

import java.time.LocalDate;
import java.util.Optional;

public interface OrderDateParser {

	Optional<LocalDate> parse(String value);
}
