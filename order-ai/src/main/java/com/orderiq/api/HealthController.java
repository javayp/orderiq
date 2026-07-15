package com.orderiq.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class HealthController {

	@GetMapping(value = "/healthz", produces = MediaType.TEXT_PLAIN_VALUE)
	ResponseEntity<String> health() {
		return ResponseEntity.ok("ok");
	}
}
