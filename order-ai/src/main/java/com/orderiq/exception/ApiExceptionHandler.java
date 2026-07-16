package com.orderiq.exception;

import com.orderiq.data.exception.InvalidOrderQueryException;
import com.orderiq.data.exception.OrderSqlExecutionException;
import com.orderiq.planning.OrderSqlValidationException;
import com.orderiq.semantic.SemanticSearchUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
final class ApiExceptionHandler {

	@ExceptionHandler(InvalidOrderQueryException.class)
	ProblemDetail invalidOrderQuery(InvalidOrderQueryException exception) {
		return ApiProblemFactory.create(HttpStatus.BAD_REQUEST, "Invalid order query", exception.getMessage());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ProblemDetail missingRequestParameter(MissingServletRequestParameterException exception) {
		log.warn("ORDER_REQUEST status=REJECTED missing_parameter=\"{}\"",
				exception.getParameterName());
		return ApiProblemFactory.create(
				HttpStatus.BAD_REQUEST,
				"Invalid order query",
				"%s is required".formatted(exception.getParameterName()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail invalidRequestParameterType(MethodArgumentTypeMismatchException exception) {
		log.warn("ORDER_REQUEST status=REJECTED parameter=\"{}\" reason=\"must be an integer\"",
				exception.getName());
		return ApiProblemFactory.create(
				HttpStatus.BAD_REQUEST,
				"Invalid order query",
				"%s must be an integer".formatted(exception.getName()));
	}

	@ExceptionHandler(OrderQuestionRejectedException.class)
	ProblemDetail rejectedOrderQuestion(OrderQuestionRejectedException exception) {
		ProblemDetail problem = ApiProblemFactory.create(
				HttpStatus.BAD_REQUEST,
				"Order question rejected",
				exception.getMessage());
		problem.setProperty("decision", exception.decision().name());
		return problem;
	}

	@ExceptionHandler({OrderSqlValidationException.class, OrderSqlExecutionException.class})
	ProblemDetail queryExecutionFailed(RuntimeException ignored) {
		return ApiProblemFactory.create(
				HttpStatus.BAD_GATEWAY,
				"Order query execution failed",
				"Unable to generate an executable order query after one retry.");
	}

	@ExceptionHandler(SemanticSearchUnavailableException.class)
	ProblemDetail semanticSearchUnavailable(SemanticSearchUnavailableException ignored) {
		return ApiProblemFactory.create(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Semantic search unavailable",
				"The semantic order index is not ready. Try again shortly.");
	}
}
