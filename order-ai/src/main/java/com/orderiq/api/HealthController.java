package com.orderiq.api;

import com.orderiq.semantic.InMemoryOrderSemanticIndex;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class HealthController {

	private final InMemoryOrderSemanticIndex semanticIndex;

	HealthController(InMemoryOrderSemanticIndex semanticIndex) {
		this.semanticIndex = semanticIndex;
	}

	@GetMapping(value = "/healthz", produces = MediaType.TEXT_PLAIN_VALUE)
	ResponseEntity<String> health() {
		return ResponseEntity.ok("ok");
	}

	@GetMapping(value = "/readyz", produces = MediaType.TEXT_PLAIN_VALUE)
	ResponseEntity<String> readiness() {
		if (!semanticIndex.isReady()) {
			return ResponseEntity.status(503).body("not ready");
		}
		return ResponseEntity.ok("ready");
	}
}
