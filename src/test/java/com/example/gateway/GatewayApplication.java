package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import com.example.aggregator.OpenApiAggregatorSpecs;
import com.example.aggregator.OpenApiAggregatorSpecs.Spec;

@SpringBootApplication
public class GatewayApplication {

	private String dates = "https://date.nager.at";
	private String wizards = "https://wizard-world-api.herokuapp.com";

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
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

	@Bean
	OpenApiAggregatorSpecs specs() {
		return new OpenApiAggregatorSpecs()
				.spec(new Spec(dates + "/swagger/v3/swagger.json").replace("/api/v3", "/dates"))
				.spec(new Spec(wizards + "/swagger/v1/swagger.json").prefix("/wizards"));
	}

}