package com.orderiq.data.service.impl;

import com.orderiq.data.exception.InvalidOrderQueryException;
import com.orderiq.data.model.Order;
import com.orderiq.data.model.OrderStatistics;
import com.orderiq.data.repository.OrderQueryRepository;
import com.orderiq.data.service.OrderQueryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultOrderQueryServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
			Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void createsAnInclusiveRecentWindowEndingToday() {
		CapturingRepository repository = new CapturingRepository();
		OrderQueryService service = new DefaultOrderQueryService(repository, FIXED_CLOCK);

		service.recent(3);

		assertThat(repository.fromInclusive).isEqualTo(LocalDate.parse("2026-07-12"));
		assertThat(repository.toInclusive).isEqualTo(LocalDate.parse("2026-07-14"));
	}

	@Test
	void normalizesCustomerIdBeforeQuerying() {
		CapturingRepository repository = new CapturingRepository();
		OrderQueryService service = new DefaultOrderQueryService(repository, FIXED_CLOCK);

		service.forCustomer("  C001  ");

		assertThat(repository.customerId).isEqualTo("C001");
	}

	@Test
	void rejectsInvalidInputsBeforeTheyReachPersistence() {
		OrderQueryService service = new DefaultOrderQueryService(new CapturingRepository(), FIXED_CLOCK);

		assertThatThrownBy(() -> service.recent(0))
				.isInstanceOf(InvalidOrderQueryException.class)
				.hasMessage("days must be greater than zero");
		assertThatThrownBy(() -> service.forCustomer("  "))
				.isInstanceOf(InvalidOrderQueryException.class)
				.hasMessage("customer_id must not be blank");
	}

	private static final class CapturingRepository implements OrderQueryRepository {
		private String customerId;
		private LocalDate fromInclusive;
		private LocalDate toInclusive;

		@Override
		public List<Order> findAll() {
			return List.of();
		}

		@Override
		public List<Order> findByCustomerId(String customerId) {
			this.customerId = customerId;
			return List.of();
		}

		@Override
		public List<Order> findBetween(LocalDate fromInclusive, LocalDate toInclusive) {
			this.fromInclusive = fromInclusive;
			this.toInclusive = toInclusive;
			return List.of();
		}

		@Override
		public OrderStatistics statistics() {
			return new OrderStatistics(BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
		}

		@Override
		public long datasetRevision() {
			return 0;
		}
	}
}
