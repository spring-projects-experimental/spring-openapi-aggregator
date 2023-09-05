package com.example.gateway;

import java.net.URI;
import java.util.Optional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		var dates = "https://date.nager.at";
		var wizards = "https://wizard-world-api.herokuapp.com";
		return rlb
				.routes()
				.route(r -> r
						.path("/dates/**")
						.filters(f -> f.stripPrefix(1).prefixPath("/api/v3"))
						.uri(dates))
				.route(r -> r
						.path("/wizards/**")
						.filters(f -> f.stripPrefix(1))
						.uri(wizards))
				.build();
	}
}
