package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import com.example.aggregator.OpenApiAggregator;
import com.example.aggregator.OpenApiAggregatorSpecs;
import com.example.aggregator.OpenApiAggregatorSpecs.Spec;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenApiAggregatorTests {

	private ObjectMapper mapper = Json.mapper();
	private OpenAPI base = new OpenAPI();

	{
		base.setInfo(new Info().title("Test").version("v0"));
	}

	@Test
	public void testSingleSpec() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("openapi.json"))),
				base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/generated", "/manual");
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		// System.err.println(mapper.writeValueAsString(api));
		SwaggerParseResult result = parser.readContents(mapper.writeValueAsString(api));
		assertThat(result.getMessages()).isEmpty();
	}

	@Test
	public void testPrefix() {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v1")),
				new OpenAPI());
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/v1/generated", "/v1/manual");
	}

	@Test
	public void testOperationPrefixComponentsLinksOperationId() throws Exception {
		// System.err.println(new ByteArrayResource("foo".getBytes()).getURL());
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("links.json")).operationPrefix("v1")),
				base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths().get("/manual").getGet().getResponses().get("200").getLinks().get("message").get$ref()).isEqualTo("#/components/links/message");
		assertThat(api.getComponents().getLinks().get("message").getOperationId()).isEqualTo("v1message");
	}

	@Test
	public void testTwoVersions() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs()
						.spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v1").operationPrefix("V1"))
						.spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v2")),
				base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/v1/generated", "/v1/manual");
		assertThat(api.getPaths()).containsKeys("/v2/generated", "/v2/manual");
		assertThat(api.getPaths().get("/v1/manual").getGet().getOperationId()).startsWith("V1");
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		SwaggerParseResult result = parser.readContents(mapper.writeValueAsString(api), null, new ParseOptions());
		assertThat(result.getMessages()).isEmpty();
	}

}
