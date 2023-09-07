package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.example.aggregator.OpenApiAggregator;
import com.example.aggregator.OpenApiAggregatorSpecs;
import com.example.aggregator.OpenApiAggregatorSpecs.Spec;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenApiAggregatorTests {

	private ObjectMapper mapper = new ObjectMapper();
	OpenAPI base = new OpenAPI();
	
	{
		base.setInfo(new Info().title("Test").version("v0"));
		mapper.setDefaultPropertyInclusion(
				JsonInclude.Value.construct(JsonInclude.Include.NON_DEFAULT, JsonInclude.Include.NON_NULL));
	}

	@Test
	public void testSingleSpec() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("openapi.json"))), 
				mapper, base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/generated", "/manual");
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		// System.err.println(mapper.writeValueAsString(api));
		SwaggerParseResult result = parser.readContents(mapper.writeValueAsString(api), null, new ParseOptions());
		assertThat(result.getMessages()).isEmpty();
	}

	@Test
	public void testPrefix() {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v1")),
				mapper,
				new OpenAPI());
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/v1/generated", "/v1/manual");
	}

	@Test
	public void testTwoVersions() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs()
						.spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v1"))
						.spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v2")),
				mapper, base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/v1/generated", "/v1/manual");
		assertThat(api.getPaths()).containsKeys("/v2/generated", "/v2/manual");
		// TODO: fail if the operationIds are duplicated
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		SwaggerParseResult result = parser.readContents(mapper.writeValueAsString(api), null, new ParseOptions());
		assertThat(result.getMessages()).anyMatch(str -> str.contains("operationId is repeated"));
	}

}
