package com.orderiq.exception;

import com.orderiq.data.exception.InvalidOrderQueryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class ApiExceptionHandler {

	@ExceptionHandler(InvalidOrderQueryException.class)
	ProblemDetail invalidOrderQuery(InvalidOrderQueryException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid order query");
		return problem;
	}

	@ExceptionHandler(OrderQuestionRejectedException.class)
	ProblemDetail rejectedOrderQuestion(OrderQuestionRejectedException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Order question rejected");
		problem.setProperty("decision", exception.decision().name());
		return problem;
	}
}
