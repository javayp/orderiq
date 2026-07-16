package com.orderiq.api;

import com.orderiq.api.model.SemanticOrderResponse;
import com.orderiq.data.exception.InvalidOrderQueryException;
import com.orderiq.exception.OrderQuestionRejectedException;
import com.orderiq.semantic.SemanticOrderMatch;
import com.orderiq.semantic.SemanticSearchProperties;
import com.orderiq.semantic.SemanticSearchUnavailableException;
import com.orderiq.service.OrderSemanticSearchService;
import com.orderiq.util.TextSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/orders")
@ConditionalOnProperty(prefix = "orderiq.semantic", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
final class OrderSemanticSearchController {

	private final OrderSemanticSearchService semanticSearchService;
	private final SemanticSearchProperties properties;

	@GetMapping("/semantic_search")
	List<SemanticOrderResponse> search(
			@RequestParam("q") String query,
			@RequestParam(value = "top_k", defaultValue = "5") int topK) {
		String logSafeQuery = TextSupport.boundedSingleLine(
				query, properties.maxQueryLength(), "<blank>");
		log.info("ORDER_SEMANTIC_SEARCH status=REQUEST question=\"{}\" top_k={}",
				logSafeQuery, topK);

		try {
			return searchAndLogAnswer(query, topK, logSafeQuery);
		} catch (InvalidOrderQueryException exception) {
			log.warn("ORDER_SEMANTIC_SEARCH status=REJECTED question=\"{}\" reason=\"{}\"",
					logSafeQuery, TextSupport.singleLine(exception.getMessage()));
			throw exception;
		} catch (OrderQuestionRejectedException exception) {
			log.warn("ORDER_SEMANTIC_SEARCH status=REJECTED question=\"{}\" reason=\"{}\"",
					logSafeQuery, TextSupport.singleLine(exception.getMessage()));
			throw exception;
		} catch (SemanticSearchUnavailableException exception) {
			log.error("ORDER_SEMANTIC_SEARCH status=FAILED question=\"{}\" reason=\"{}\"",
					logSafeQuery, TextSupport.singleLine(exception.getMessage()));
			throw exception;
		}
	}

	private List<SemanticOrderResponse> searchAndLogAnswer(
			String query,
			int topK,
			String logSafeQuery) {
		List<SemanticOrderMatch> matches = semanticSearchService.search(query, topK);
		List<SemanticOrderResponse> response = new ArrayList<>(matches.size());
		for (SemanticOrderMatch match : matches) {
			response.add(SemanticOrderResponse.from(match));
		}
		List<SemanticOrderResponse> answer = List.copyOf(response);
		log.info("ORDER_SEMANTIC_SEARCH status=SUCCESS question=\"{}\" answer={}",
				logSafeQuery, answer);
		return answer;
	}
}
