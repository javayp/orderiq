package com.orderiq.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

final class ApiProblemFactory {

	private ApiProblemFactory() {
	}

	static ProblemDetail create(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
