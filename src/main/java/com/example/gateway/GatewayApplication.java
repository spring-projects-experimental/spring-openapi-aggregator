package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gateway.OpenApiAggregatorSpecs.Spec;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer objectMapperConfigurer() {
		return builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL);
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

@RestController
class AggregatorEndpoint {
	private final OpenApiAggregator aggregator;

	public AggregatorEndpoint(OpenApiAggregator aggregator) {
		this.aggregator = aggregator;
	}

	@GetMapping("/v3/api-docs")
	public Mono<OpenAPI> api() {
		return aggregator.aggregate();
	}
}