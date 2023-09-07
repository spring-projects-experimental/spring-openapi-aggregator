package com.example.aggregator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import reactor.core.publisher.Mono;

@Configuration
@ConditionalOnBean(OpenApiAggregatorSpecs.class)
public class OpenApiAggregatorConfiguration {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer objectMapperConfigurer() {
		return builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL)
				.serializationInclusion(JsonInclude.Include.NON_DEFAULT);
	}

	@Bean
	public OpenApiAggregator openApiAggregator(OpenApiAggregatorSpecs specs, ObjectMapper rest) {
		return new OpenApiAggregator(specs, rest);
	}

	@Bean
	public AggregatorEndpoint aggregatorEndpoint(OpenApiAggregator aggregator) {
		return new AggregatorEndpoint(aggregator);
	}

}

@RestController
class AggregatorEndpoint implements InitializingBean {
	private final OpenApiAggregator aggregator;
	private OpenAPI api;

	public AggregatorEndpoint(OpenApiAggregator aggregator) {
		this.aggregator = aggregator;
	}

	@GetMapping("${spring.openapi.aggregator.path:/v3/api-docs}")
	public Mono<OpenAPI> api() {
		return Mono.just(api);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.api = aggregator.aggregate();
	}
}