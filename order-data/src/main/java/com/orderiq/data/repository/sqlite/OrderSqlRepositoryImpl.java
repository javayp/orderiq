package com.orderiq.data.repository.sqlite;

import com.orderiq.data.exception.OrderSqlExecutionException;
import com.orderiq.data.repository.OrderSqlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class OrderSqlRepositoryImpl implements OrderSqlRepository {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public List<Map<String, Object>> executeQuery(String sql) {
		try {
			return jdbcTemplate.queryForList(sql);
		} catch (DataAccessException exception) {
			throw new OrderSqlExecutionException(errorMessage(exception), exception);
		}
	}

	private static String errorMessage(DataAccessException exception) {
		String message = exception.getMostSpecificCause().getMessage();
		return message == null || message.isBlank()
				? "SQLite query execution failed"
				: message;
	}
}
