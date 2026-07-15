package com.orderiq.data.config;

import com.orderiq.data.model.Order;
import com.orderiq.data.model.OrderStatistics;
import com.orderiq.data.port.OrderQueryRepository;
import com.orderiq.data.port.OrderStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDataConfigurationTest {

	@TempDir
	Path tempDirectory;

	@Test
	void supportsTheDocumentedOrderIqDbPathEnvironmentVariable() {
		Path configuredPath = tempDirectory.resolve("configured-orders.db").toAbsolutePath();

		new ApplicationContextRunner()
				.withUserConfiguration(OrderDataConfiguration.class)
				.withPropertyValues("ORDERIQ_DB_PATH=%s".formatted(configuredPath))
				.withBean(OrderStore.class, () -> orders -> { })
				.withBean(OrderQueryRepository.class, EmptyOrderQueryRepository::new)
				.run(context -> {
					assertThat(context).hasNotFailed();
					SQLiteDataSource dataSource = context.getBean(SQLiteDataSource.class);
					try (var connection = dataSource.getConnection()) {
						assertThat(connection.getMetaData().getURL())
								.isEqualTo("jdbc:sqlite:%s".formatted(configuredPath));
					}
				});
	}

	private static final class EmptyOrderQueryRepository implements OrderQueryRepository {

		@Override
		public List<Order> findByCustomerId(String customerId) {
			return List.of();
		}

		@Override
		public List<Order> findBetween(LocalDate fromInclusive, LocalDate toInclusive) {
			return List.of();
		}

		@Override
		public OrderStatistics statistics() {
			return new OrderStatistics(BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
		}
	}
}
