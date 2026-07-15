package com.orderiq.data.adapter.csv;

import com.orderiq.data.exception.IngestionException;
import com.orderiq.data.model.RawOrderRow;
import com.orderiq.data.port.OrderSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CsvOrderReader implements OrderSource {

	private static final Set<String> REQUIRED_HEADERS = Set.of(
			"order_id", "customer_id", "order_date", "amount", "currency");

	private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.setIgnoreEmptyLines(true)
			.setIgnoreSurroundingSpaces(true)
			.setTrim(true)
			.get();

	@Override
	public List<RawOrderRow> read(Path source) {
		if (!Files.isRegularFile(source)) {
			throw new IngestionException(
					"CSV file does not exist or is not a regular file: %s".formatted(source));
		}
		try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8);
			 CSVParser parser = CSV_FORMAT.parse(reader)) {
			validateHeaders(source, parser.getHeaderMap());
			return parser.stream()
					.map(CsvOrderReader::toRawOrderRow)
					.toList();
		} catch (IOException | IllegalArgumentException exception) {
			throw new IngestionException(
					"Unable to read CSV file %s: %s".formatted(source, exception.getMessage()),
					exception);
		}
	}

	private static RawOrderRow toRawOrderRow(CSVRecord record) {
		return new RawOrderRow(
				record.getRecordNumber() + 1,
				record.get("order_id"),
				record.get("customer_id"),
				record.get("order_date"),
				record.get("amount"),
				record.get("currency"));
	}

	private static void validateHeaders(Path source, Map<String, Integer> headers) {
		if (!headers.keySet().containsAll(REQUIRED_HEADERS)) {
			Set<String> missing = new java.util.TreeSet<>(REQUIRED_HEADERS);
			missing.removeAll(headers.keySet());
			throw new IngestionException(
					"CSV file %s is missing required headers: %s".formatted(source, missing));
		}
	}
}
