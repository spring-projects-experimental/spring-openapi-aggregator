package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.example.aggregator.OpenApiAggregator;
import com.example.aggregator.OpenApiAggregatorSpecs;
import com.example.aggregator.OpenApiAggregatorSpecs.Spec;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenApiAggregatorTests {
	@Test
	public void testSingleSpec() {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("openapi.json"))), new ObjectMapper(),
				new OpenAPI());
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/generated", "/manual");
	}

	@Test
	public void testPrefix() {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v1")),
				new ObjectMapper(),
				new OpenAPI());
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/v1/generated", "/v1/manual");
	}

	@Test
	public void testTwoVersions() {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs()
						.spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v1"))
						.spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v2")),
				new ObjectMapper(),
				new OpenAPI());
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/v1/generated", "/v1/manual");
		assertThat(api.getPaths()).containsKeys("/v2/generated", "/v2/manual");
		// TODO: fail if the operationIds are duplicated
	}
}
