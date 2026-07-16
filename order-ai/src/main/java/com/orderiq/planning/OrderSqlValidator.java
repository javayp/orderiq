package com.orderiq.planning;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public final class OrderSqlValidator {

	private static final Pattern FORBIDDEN_OPERATION = Pattern.compile(
			"(?i)\\b(?:INSERT|UPDATE|DELETE|REPLACE|DROP|ALTER|CREATE|TRUNCATE|PRAGMA|ATTACH|DETACH|VACUUM|REINDEX)\\b");
	private static final Pattern INTERNAL_SQLITE_TABLE = Pattern.compile(
			"(?i)\\bsqlite_(?:master|schema|temp_master)\\b");
	private static final Pattern READ_ONLY_QUERY = Pattern.compile("(?i)^(?:SELECT|WITH)\\b");

	public void validate(String sql) {
		// A generated query must contain executable SQL.
		if (sql == null || sql.isBlank()) {
			throw new OrderSqlValidationException("Generated SQL must not be blank");
		}

		// Ignore quoted values while checking the SQL structure around them.
		String structure = maskSingleQuotedValues(sql.strip());

		// Apply each read-only safety rule before the query reaches SQLite.
		validateComments(structure);
		String statement = removeTrailingSemicolon(structure);
		validateSingleStatement(statement);
		validateReadOnlyQuery(statement);
		validateRestrictedOperations(statement);
	}

	/** Rejects SQL comments outside quoted text values. */
	private static void validateComments(String sql) {
		if (sql.contains("--") || sql.contains("/*") || sql.contains("*/")) {
			throw new OrderSqlValidationException("SQL comments are not allowed");
		}
	}

	/** Removes the optional terminator from one otherwise complete statement. */
	private static String removeTrailingSemicolon(String sql) {
		String statement = sql.stripTrailing();
		if (statement.endsWith(";")) {
			return statement.substring(0, statement.length() - 1).stripTrailing();
		}
		return statement;
	}

	/** Rejects any remaining semicolon because it indicates another statement. */
	private static void validateSingleStatement(String sql) {
		if (sql.indexOf(';') >= 0) {
			throw new OrderSqlValidationException("Only one SQL statement is allowed");
		}
	}

	/** Allows only SELECT or WITH as the first SQL keyword, regardless of whitespace. */
	private static void validateReadOnlyQuery(String sql) {
		if (!READ_ONLY_QUERY.matcher(sql.stripLeading()).find()) {
			throw new OrderSqlValidationException("Only SELECT queries are allowed");
		}
	}

	/** Blocks write operations and access to SQLite's internal metadata tables. */
	private static void validateRestrictedOperations(String sql) {
		if (FORBIDDEN_OPERATION.matcher(sql).find()) {
			throw new OrderSqlValidationException("SQL contains a prohibited operation");
		}
		if (INTERNAL_SQLITE_TABLE.matcher(sql).find()) {
			throw new OrderSqlValidationException("SQLite internal tables are not available");
		}
	}

	/** Masks quoted values so their contents cannot be mistaken for SQL structure. */
	private static String maskSingleQuotedValues(String sql) {
		StringBuilder masked = new StringBuilder(sql.length());
		boolean insideValue = false;

		for (int index = 0; index < sql.length(); index++) {
			char character = sql.charAt(index);
			if (character == '\'' && insideValue && isEscapedQuote(sql, index)) {
				masked.append("  ");
				index++;
				continue;
			}
			if (character == '\'') {
				insideValue = !insideValue;
				masked.append(' ');
				continue;
			}
			masked.append(insideValue ? ' ' : character);
		}

		if (insideValue) {
			throw new OrderSqlValidationException("SQL contains an unterminated text value");
		}
		return masked.toString();
	}

	/** Recognizes SQLite's doubled quote escape inside a text value. */
	private static boolean isEscapedQuote(String sql, int index) {
		return index + 1 < sql.length() && sql.charAt(index + 1) == '\'';
	}
}
