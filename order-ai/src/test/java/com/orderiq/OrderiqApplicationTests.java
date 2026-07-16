package com.orderiq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@SpringBootTest
class OrderiqApplicationTests {
	@TempDir
	static Path tempDirectory;

	@DynamicPropertySource
	static void databasePath(DynamicPropertyRegistry registry) {
		registry.add("orderiq.database.path", () -> tempDirectory.resolve("orders.db").toString());
		registry.add("orderiq.semantic.enabled", () -> "false");
		registry.add("spring.ai.model.embedding", () -> "none");
	}

	@Test
	void contextLoads() {
	}

}
