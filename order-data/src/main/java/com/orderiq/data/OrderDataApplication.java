package com.orderiq.data;

import com.orderiq.data.adapter.sqlite.SqliteOrderQueryRepository;
import com.orderiq.data.adapter.sqlite.SqliteOrderStore;
import com.orderiq.data.cli.OrderIngestionCli;
import com.orderiq.data.config.OrderDataConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.util.Map;

@EnableAutoConfiguration
@Import({
		OrderDataConfiguration.class,
		SqliteOrderStore.class,
		SqliteOrderQueryRepository.class,
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
