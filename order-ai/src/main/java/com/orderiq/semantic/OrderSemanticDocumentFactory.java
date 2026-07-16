package com.orderiq.semantic;

import com.orderiq.data.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public final class OrderSemanticDocumentFactory {

	private static final double LOW_PERCENTILE = 0.25;
	private static final double VERY_LOW_PERCENTILE = 0.10;
	private static final double HIGH_PERCENTILE = 0.75;
	private static final double VERY_HIGH_PERCENTILE = 0.90;

	public List<String> createDocuments(List<Order> orders) {
		if (orders.isEmpty()) {
			return List.of();
		}

		DatasetBoundaries boundaries = boundariesFor(orders);
		List<String> documents = new ArrayList<>(orders.size());
		for (Order order : orders) {
			documents.add(toDocument(order, boundaries));
		}
		return List.copyOf(documents);
	}

	private static DatasetBoundaries boundariesFor(List<Order> orders) {
		List<BigDecimal> amounts = new ArrayList<>(orders.size());
		List<LocalDate> dates = new ArrayList<>(orders.size());
		for (Order order : orders) {
			amounts.add(order.amountUsd());
			dates.add(order.orderDate());
		}
		amounts.sort(Comparator.naturalOrder());
		dates.sort(Comparator.naturalOrder());

		return new DatasetBoundaries(
				percentile(amounts, VERY_LOW_PERCENTILE),
				percentile(amounts, LOW_PERCENTILE),
				percentile(amounts, HIGH_PERCENTILE),
				percentile(amounts, VERY_HIGH_PERCENTILE),
				percentile(dates, VERY_LOW_PERCENTILE),
				percentile(dates, LOW_PERCENTILE),
				percentile(dates, HIGH_PERCENTILE),
				percentile(dates, VERY_HIGH_PERCENTILE));
	}

	private static <T> T percentile(List<T> sortedValues, double percentile) {
		int rank = (int) Math.ceil(percentile * sortedValues.size());
		int index = Math.max(0, rank - 1);
		return sortedValues.get(index);
	}

	private static String toDocument(Order order, DatasetBoundaries boundaries) {
		return "order %s, customer %s, amount %s USD, %s, date %s, %s, %s, customer purchase transaction"
				.formatted(
						order.orderId(),
						order.customerId(),
						order.amountUsd().toPlainString(),
						valueDescription(order.amountUsd(), boundaries),
						order.orderDate(),
						dateDescription(order.orderDate(), boundaries),
						combinedDescription(order, boundaries));
	}

	private static String valueDescription(BigDecimal amount, DatasetBoundaries boundaries) {
		if (amount.compareTo(boundaries.veryHighAmount()) >= 0) {
			return "very high value unusually expensive large premium";
		}
		if (amount.compareTo(boundaries.highAmount()) >= 0) {
			return "high value expensive large";
		}
		if (amount.compareTo(boundaries.veryLowAmount()) <= 0) {
			return "very low value small tiny inexpensive";
		}
		if (amount.compareTo(boundaries.lowAmount()) <= 0) {
			return "low value small inexpensive";
		}
		return "medium value standard";
	}

	private static String combinedDescription(Order order, DatasetBoundaries boundaries) {
		boolean veryHighValue = order.amountUsd().compareTo(boundaries.veryHighAmount()) >= 0;
		boolean highValue = order.amountUsd().compareTo(boundaries.highAmount()) >= 0;
		boolean recent = order.orderDate().compareTo(boundaries.recentDate()) >= 0;

		if (veryHighValue && recent) {
			return "combined category very high value recent premium";
		}
		if (highValue && recent) {
			return "combined category high value recent";
		}
		return "independent value and date profile";
	}

	private static String dateDescription(LocalDate date, DatasetBoundaries boundaries) {
		if (date.compareTo(boundaries.veryRecentDate()) >= 0) {
			return "very recent latest newest";
		}
		if (date.compareTo(boundaries.recentDate()) >= 0) {
			return "recent new";
		}
		if (date.compareTo(boundaries.veryOldDate()) <= 0) {
			return "very old historical earliest";
		}
		if (date.compareTo(boundaries.oldDate()) <= 0) {
			return "old historical";
		}
		return "established date";
	}

	private record DatasetBoundaries(
			BigDecimal veryLowAmount,
			BigDecimal lowAmount,
			BigDecimal highAmount,
			BigDecimal veryHighAmount,
			LocalDate veryOldDate,
			LocalDate oldDate,
			LocalDate recentDate,
			LocalDate veryRecentDate) {
	}
}
