package com.orderiq.guardrail;

import com.orderiq.guardrail.OrderQueryFrame.Decision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Stateless, deterministic question guardrail used before the LLM. */
@Component
@RequiredArgsConstructor
public final class OrderQuestionGuardrail {

	private final OrderVocabularyConfiguration configuration;
	private final OrderIntentAnalyzer analyzer;

	public OrderQueryFrame evaluate(String question) {
		String original = QuestionText.require(question);
		String normalized = QuestionText.normalize(original);
		Optional<OrderQueryFrame> earlyRejection = earlyRejection(original, normalized);
		if (earlyRejection.isPresent()) {
			return earlyRejection.get();
		}

		OrderIntentEvidence evidence = analyzer.analyze(original, normalized);
		Decision decision = decision(normalized, evidence);

		return new OrderQueryFrame(
				original,
				normalized,
				decision,
				evidence.metrics(),
				evidence.groupings(),
				evidence.sort(),
				evidence.customerIds(),
				evidence.orderId(),
				evidence.unsupported(),
				reason(decision, evidence));
	}

	/** Reuses the configured vocabulary for short semantic-search fragments. */
	public boolean hasSupportedOrderEvidence(String question) {
		String original = QuestionText.require(question);
		String normalized = QuestionText.normalize(original);
		OrderIntentEvidence evidence = analyzer.analyze(original, normalized);
		return evidence.unsupported().isEmpty()
				&& analyzer.hasOrderEvidence(original, normalized);
	}

	private Optional<OrderQueryFrame> earlyRejection(String original, String normalized) {
		if (VocabularyMatcher.contains(
				QuestionText.securityText(normalized), configuration.securityIndicators())) {
			return Optional.of(OrderQueryFrame.rejected(
					original,
					normalized,
					Decision.SECURITY_REJECTED,
					"Potential prompt or SQL injection was detected."));
		}
		if (VocabularyMatcher.contains(normalized, configuration.blockedContent())) {
			return Optional.of(OrderQueryFrame.rejected(
					original,
					normalized,
					Decision.CONTENT_REJECTED,
					"Abusive content is not accepted."));
		}
		if (hasMultipleQuestions(original)
				|| hasIndependentIntent(normalized)) {
			return Optional.of(OrderQueryFrame.rejected(
					original,
					normalized,
					Decision.MULTIPLE_QUESTIONS,
					"Submit one order question at a time."));
		}
		return Optional.empty();
	}

	private Decision decision(String question, OrderIntentEvidence evidence) {
		if (!evidence.recognized()) {
			return Decision.OUT_OF_DOMAIN;
		}
		if (!evidence.unsupported().isEmpty()) {
			return Decision.UNSUPPORTED_SCHEMA;
		}
		if (VocabularyMatcher.endsWith(question, configuration.incompleteSuffixes())) {
			return Decision.INCOMPLETE_QUESTION;
		}
		return Decision.ALLOWED;
	}

	private static String reason(Decision decision, OrderIntentEvidence evidence) {
		return switch (decision) {
			case OUT_OF_DOMAIN -> "No supported order-domain concept was recognized.";
			case UNSUPPORTED_SCHEMA -> "The orders schema does not contain: %s."
					.formatted(String.join(", ", evidence.unsupported()));
			case INCOMPLETE_QUESTION -> "The order question is incomplete.";
			default -> "";
		};
	}

	private boolean hasMultipleQuestions(String question) {
		String[] clauses = QuestionText.clauses(question);
		boolean firstClauseFound = false;

		for (String clause : clauses) {
			if (clause.isBlank()) {
				continue;
			}
			if (!firstClauseFound) {
				firstClauseFound = true;
				continue;
			}
			if (!isOrderContinuation(clause)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasIndependentIntent(String question) {
		for (String marker : configuration.multipleQuestionMarkers()) {
			int searchFrom = 0;
			int markerEnd = VocabularyMatcher.nextMatchEnd(question, marker, searchFrom);
			while (markerEnd >= 0) {
				String remainder = question.substring(markerEnd).strip();
				boolean explicitSeparator = !Character.isLetterOrDigit(marker.charAt(0));
				if (explicitSeparator || !analyzer.hasOrderEvidence(remainder, remainder)) {
					return true;
				}
				searchFrom = markerEnd;
				markerEnd = VocabularyMatcher.nextMatchEnd(question, marker, searchFrom);
			}
		}
		return false;
	}

	private boolean isOrderContinuation(String clause) {
		String normalized = QuestionText.normalize(clause);
		String matchingPrefix = longestContinuationPrefix(normalized);
		if (matchingPrefix == null) {
			return false;
		}

		String remainder = normalized.substring(matchingPrefix.length()).strip();
		return analyzer.hasOrderEvidence(clause, normalized)
				|| QuestionText.containsDigit(remainder)
				|| VocabularyMatcher.contains(remainder, configuration.incompleteSuffixes());
	}

	private String longestContinuationPrefix(String question) {
		String matchingPrefix = null;
		for (String marker : configuration.continuationMarkers()) {
			boolean matches = question.equals(marker) || question.startsWith(marker.concat(" "));
			if (matches && (matchingPrefix == null || marker.length() > matchingPrefix.length())) {
				matchingPrefix = marker;
			}
		}
		return matchingPrefix;
	}

}
