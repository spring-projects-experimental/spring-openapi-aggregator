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

	@Bean
	OpenApiAggregatorSpecs specs() {
		return new OpenApiAggregatorSpecs()
				.spec(new Spec("https://date.nager.at/swagger/v3/swagger.json").replace("/api/v3", "/dates"))
				.spec(new Spec("https://wizard-world-api.herokuapp.com/swagger/v1/swagger.json").prefix("/wizards"));
	}

}