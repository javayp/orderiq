package com.orderiq.api;

import com.orderiq.semantic.InMemoryOrderSemanticIndex;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

	@Test
	void readinessTracksTheSemanticIndexLifecycle() {
		InMemoryOrderSemanticIndex index = new InMemoryOrderSemanticIndex();
		HealthController controller = new HealthController(index);

		assertThat(controller.readiness().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(controller.readiness().getBody()).isEqualTo("not ready");

		index.replace(List.of(), List.of());

		assertThat(controller.readiness().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(controller.readiness().getBody()).isEqualTo("ready");
	}
}
