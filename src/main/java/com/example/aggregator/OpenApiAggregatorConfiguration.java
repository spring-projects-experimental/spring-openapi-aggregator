package com.example.aggregator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@ConditionalOnBean(OpenApiAggregatorSpecs.class)
public class OpenApiAggregatorConfiguration {

	@Bean
	public OpenApiAggregator openApiAggregator(OpenApiAggregatorSpecs specs,
			@Qualifier("spring.openapi.base") OpenAPI base) {
		return new OpenApiAggregator(specs, base);
	}

	@Bean("spring.openapi.base")
	@ConfigurationProperties("spring.openapi.base")
	public OpenAPI base() {
		OpenAPI api = new OpenAPI();
		api.paths(new Paths());
		api.components(new Components());
		api.info(new Info().title("Gateway API").description("Gateway API").version("1.0.0"));
		return api;
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

	private final ObjectMapper mapper = Json.mapper();

	public AggregatorEndpoint(OpenApiAggregator aggregator) {
		this.aggregator = aggregator;
		mapper.setDefaultPropertyInclusion(
				JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_DEFAULT));
	}

	@GetMapping(path = "${spring.openapi.aggregator.path:/v3/api-docs}", produces = { "application/json" })
	public String api() {
		try {
			return mapper.writeValueAsString(api);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.api = aggregator.aggregate();
	}

}