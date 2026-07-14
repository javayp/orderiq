package com.orderiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;

import java.util.Arrays;

@SpringBootApplication
public class OrderiqApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(OrderiqApplication.class);
		if (Arrays.stream(args).findFirst().filter("load"::equalsIgnoreCase).isPresent()) {
			application.setWebApplicationType(WebApplicationType.NONE);
		}
		application.run(args);
	}

}
