package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenApiValidationTests {

	@Test
	public void testValidate() {
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		SwaggerParseResult api = parser.readLocation("src/test/resources/openapi.json", null, new ParseOptions());
		assertThat(api.getMessages()).isEmpty();
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
