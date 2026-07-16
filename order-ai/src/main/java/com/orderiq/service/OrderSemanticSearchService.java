package com.orderiq.service;

import com.orderiq.semantic.SemanticOrderMatch;

import java.util.List;

public interface OrderSemanticSearchService {

	List<SemanticOrderMatch> search(String query, int topK);
}
