package com.orderiq.planning;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderSqlValidatorTest {

	private final OrderSqlValidator validator = new OrderSqlValidator();

	@Test
	void acceptsOneReadOnlySelectWithTrailingSemicolon() {
		assertThatCode(() -> validator.validate("""
				SELECT customer_id, COUNT(*) AS total_orders
				FROM orders
				GROUP BY customer_id;
				"""))
				.doesNotThrowAnyException();
	}

	@Test
	void acceptsWhitespaceAfterTheSelectKeyword() {
		assertThatCode(() -> validator.validate("""
				SELECT
					customer_id,
					ROUND(SUM(amount_usd), 2) AS total_revenue
				FROM orders
				GROUP BY customer_id
				"""))
				.doesNotThrowAnyException();
	}

	@Test
	void acceptsWhitespaceAfterTheWithKeyword() {
		assertThatCode(() -> validator.validate("""
				WITH
					customer_orders AS (
						SELECT amount_usd FROM orders WHERE customer_id = 'EP-13915'
					)
				SELECT ROUND(SUM(amount_usd), 2) FROM customer_orders
				"""))
				.doesNotThrowAnyException();
	}

	@Test
	void acceptsRestrictedWordsInsideTextValues() {
		assertThatCode(() -> validator.validate("""
				SELECT order_id FROM orders WHERE customer_id = 'DELETE; --'
				"""))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsWritesAndMultipleStatements() {
		assertThatThrownBy(() -> validator.validate("DELETE FROM orders"))
				.isInstanceOf(OrderSqlValidationException.class)
				.hasMessageContaining("SELECT");

		assertThatThrownBy(() -> validator.validate("SELECT * FROM orders; DROP TABLE orders"))
				.isInstanceOf(OrderSqlValidationException.class)
				.hasMessageContaining("one SQL statement");
	}

	@Test
	void rejectsKeywordsThatOnlyStartWithSelect() {
		assertThatThrownBy(() -> validator.validate("SELECTED value FROM orders"))
				.isInstanceOf(OrderSqlValidationException.class)
				.hasMessageContaining("SELECT");
	}

	@Test
	void rejectsSqliteInternalTables() {
		assertThatThrownBy(() -> validator.validate("SELECT name FROM sqlite_master"))
				.isInstanceOf(OrderSqlValidationException.class)
				.hasMessageContaining("internal tables");
	}
}
