package com.orderiq.data;

import com.orderiq.data.cli.OrderIngestionCli;
import com.orderiq.data.config.OrderDataConfiguration;
import com.orderiq.data.repository.sqlite.OrderQueryRepositoryImpl;
import com.orderiq.data.repository.sqlite.OrderStoreImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.util.Map;

@EnableAutoConfiguration
@Import({
		OrderDataConfiguration.class,
		OrderStoreImpl.class,
		OrderQueryRepositoryImpl.class,
		OrderIngestionCli.class
})
public class OrderDataApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(OrderDataApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setDefaultProperties(Map.of(
				"spring.application.name", "order-data",
				"spring.sql.init.mode", "always"));
		application.run(args);
	}
}
