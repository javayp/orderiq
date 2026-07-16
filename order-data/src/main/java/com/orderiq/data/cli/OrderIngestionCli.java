package com.orderiq.data.cli;

import com.orderiq.data.model.IngestionIssue;
import com.orderiq.data.model.IngestionReport;
import com.orderiq.data.service.OrderIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
public final class OrderIngestionCli implements ApplicationRunner {

	private final OrderIngestionService ingestionService;

	@Override
	public void run(ApplicationArguments arguments) {
		List<String> command = arguments.getNonOptionArgs();
		if (command.isEmpty()) {
			throw new IllegalArgumentException("Usage: load <csv-file> [<csv-file> ...]");
		}
		if (!"load".equalsIgnoreCase(command.getFirst())) {
			throw new IllegalArgumentException(
					"Unknown command '%s'. Supported command: load <csv-file> [<csv-file> ...]"
							.formatted(command.getFirst()));
		}
		if (command.size() < 2) {
			throw new IllegalArgumentException("Usage: load <csv-file> [<csv-file> ...]");
		}

		List<Path> sources = command.subList(1, command.size()).stream().map(Path::of).toList();
		IngestionReport report = ingestionService.load(sources);
		print(report);
	}

	private static void print(IngestionReport report) {
		System.out.printf(
				"ETL complete: read=%d, loaded=%d, dropped=%d, amount_defaults=%d, currency_defaults=%d%n",
				report.rowsRead(),
				report.rowsLoaded(),
				report.rowsDropped(),
				report.amountsDefaulted(),
				report.currenciesDefaulted());
		for (IngestionIssue issue : report.issues()) {
			System.out.printf("row=%d order_id=%s issue=%s detail=%s%n",
					issue.rowNumber(), issue.orderId(), issue.code(), issue.detail());
		}
	}
}
