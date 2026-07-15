package com.orderiq.data.config;

import com.orderiq.data.adapter.csv.CsvOrderReader;
import com.orderiq.data.policy.CurrencyConverter;
import com.orderiq.data.policy.OrderDateParser;
import com.orderiq.data.policy.impl.FixedRateCurrencyConverter;
import com.orderiq.data.policy.impl.FlexibleOrderDateParser;
import com.orderiq.data.port.OrderQueryRepository;
import com.orderiq.data.port.OrderSource;
import com.orderiq.data.port.OrderStore;
import com.orderiq.data.port.OrdersReloadedPublisher;
import com.orderiq.data.service.OrderIngestionService;
import com.orderiq.data.service.OrderQueryService;
import com.orderiq.data.service.OrderTransformer;
import com.orderiq.data.service.impl.DefaultOrderIngestionService;
import com.orderiq.data.service.impl.DefaultOrderQueryService;
import com.orderiq.data.service.impl.DefaultOrderTransformer;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class OrderDataConfiguration {

	@Bean
	Clock systemClock() {
		return Clock.systemUTC();
	}

	@Bean
	OrderSource orderSource() {
		return new CsvOrderReader();
	}

	@Bean
	OrderDateParser orderDateParser() {
		return new FlexibleOrderDateParser();
	}

	@Bean
	CurrencyConverter currencyConverter() {
		return new FixedRateCurrencyConverter(Map.of(
				"USD", BigDecimal.ONE,
				"EUR", new BigDecimal("1.10")));
	}

	@Bean
	OrderTransformer orderTransformer(
			OrderDateParser orderDateParser,
			CurrencyConverter currencyConverter) {
		return new DefaultOrderTransformer(orderDateParser, currencyConverter);
	}

	@Bean
	OrdersReloadedPublisher ordersReloadedPublisher(ApplicationEventPublisher publisher) {
		return event -> publisher.publishEvent(event);
	}

	@Bean
	OrderIngestionService orderIngestionService(
			OrderSource orderSource,
			OrderTransformer orderTransformer,
			OrderStore orderStore,
			OrdersReloadedPublisher eventPublisher,
			Clock clock) {
		return new DefaultOrderIngestionService(
				orderSource, orderTransformer, orderStore, eventPublisher, clock);
	}

	@Bean
	OrderQueryService orderQueryService(OrderQueryRepository repository, Clock clock) {
		return new DefaultOrderQueryService(repository, clock);
	}

	@Bean
	SQLiteDataSource orderDataSource(
			@Value("${orderiq.database.path:${ORDERIQ_DB_PATH:./data/orders.db}}") String configuredPath) {
		Path databasePath = Path.of(configuredPath).toAbsolutePath().normalize();
		createParentDirectory(databasePath);

		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.setBusyTimeout(5_000);
		sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
		sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
		sqliteConfig.enforceForeignKeys(true);

		SQLiteDataSource dataSource = new SQLiteDataSource(sqliteConfig);
		dataSource.setUrl("jdbc:sqlite:" + databasePath);
		return dataSource;
	}

	private static void createParentDirectory(Path databasePath) {
		Path parent = databasePath.getParent();
		if (parent == null) {
			return;
		}
		try {
			Files.createDirectories(parent);
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to create database directory " + parent, exception);
		}
	}
}
