package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIResolver;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenApiValidationTests {

	private ObjectMapper mapper = Json.mapper();

	@Test
	public void testValidate() throws Exception {
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		SwaggerParseResult api = parser.readLocation("src/test/resources/openapi.json", null, new ParseOptions());
		// System.err.println(mapper.writeValueAsString(api.getOpenAPI()));
		assertThat(api.getMessages()).isEmpty();
	}

	@Test
	public void testValidateLinks() throws Exception {
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		SwaggerParseResult api = parser.readLocation("src/test/resources/links.json", null, new ParseOptions());
		System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(api.getOpenAPI()));
		assertThat(api.getMessages()).isEmpty();
	}

	@Test
	public void testSwaggerSpec() throws Exception {
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		OpenAPI api = parser.read("src/test/resources/swagger.json", null, new ParseOptions());
		assertThat(api).isNotNull();
		assertThat(api.getPaths()).containsKeys("/generated", "/manual");
		System.err.println(mapper.writeValueAsString(api));
		SwaggerParseResult result = parser.readContents(mapper.writeValueAsString(api), null, new ParseOptions());
		assertThat(result.getMessages()).isEmpty();
	}

	@Test
	public void testValidateInvalid() {
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		SwaggerParseResult api = parser.readContents("""
				{
					"openapi": "3.0.1",
					"info": {
						"title": "Test",
						"version": "1.0.0"
					}
				}
				""", null, new ParseOptions());
		assertThat(api.getMessages()).contains("attribute paths is missing");
	}

}
