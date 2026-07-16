package com.orderiq.exception;

import com.orderiq.data.exception.InvalidOrderQueryException;
import com.orderiq.data.exception.OrderSqlExecutionException;
import com.orderiq.planning.OrderSqlValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class ApiExceptionHandler {

	@ExceptionHandler(InvalidOrderQueryException.class)
	ProblemDetail invalidOrderQuery(InvalidOrderQueryException exception) {
		return ApiProblemFactory.create(HttpStatus.BAD_REQUEST, "Invalid order query", exception.getMessage());
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
}
