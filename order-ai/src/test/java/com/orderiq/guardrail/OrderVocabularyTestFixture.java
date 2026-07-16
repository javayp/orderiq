package com.orderiq.guardrail;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

final class OrderVocabularyTestFixture {

	private static final ConfigurableEnvironment ENVIRONMENT = loadEnvironment();
	private static final OrderVocabularyConfiguration CONFIGURATION = bind(
			"orderiq.ai.guardrail.vocabulary",
			OrderVocabularyConfiguration.class);

	private OrderVocabularyTestFixture() {
	}

	static OrderQuestionGuardrail guardrail() {
		return new OrderQuestionGuardrail(
				CONFIGURATION,
				new OrderIntentAnalyzer(CONFIGURATION));
	}

	private static ConfigurableEnvironment loadEnvironment() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		String resource = "order-vocabulary.yaml";
		try {
			new YamlPropertySourceLoader()
					.load(resource, new ClassPathResource(resource))
					.forEach(environment.getPropertySources()::addFirst);
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to load %s".formatted(resource), exception);
		}
		return environment;
	}

	private static <T> T bind(String prefix, Class<T> type) {
		return Binder.get(ENVIRONMENT)
				.bind(prefix, Bindable.of(type))
				.orElseThrow(() -> new IllegalStateException(
						"%s configuration is missing".formatted(prefix)));
	}
}
