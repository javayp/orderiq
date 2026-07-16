package com.orderiq.data.repository;

import java.util.List;
import java.util.Map;

public interface OrderSqlRepository {

	List<Map<String, Object>> executeQuery(String sql);
}
