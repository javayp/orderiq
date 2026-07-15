package com.orderiq.api.model;

import java.util.List;
import java.util.Map;

public record AskOrderResponse(String answer, String sqlUsed, List<Map<String,Object>> rows) {

}
