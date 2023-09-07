package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenApiValidationTests {

	private ObjectMapper mapper = ObjectMapperFactory.createJson();

	static class SchemaMixin {
		@JsonIgnore
		public Set<String> types;
		@JsonIgnore
		public boolean exampleSetFlag;
		@JsonIgnore
		public Map<String, Object> jsonSchema;
	}

	{
		mapper.setDefaultPropertyInclusion(
				JsonInclude.Value.construct(JsonInclude.Include.NON_DEFAULT, JsonInclude.Include.NON_NULL));
		mapper.addMixIn(Schema.class, SchemaMixin.class);
	}

	@Test
	public void testValidate() throws Exception {
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		SwaggerParseResult api = parser.readLocation("src/test/resources/openapi.json", null, new ParseOptions());
		// System.err.println(mapper.writeValueAsString(api.getOpenAPI()));
		assertThat(api.getMessages()).isEmpty();
	}

	@Test
	public void testSwaggerSpec() throws Exception {
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		OpenAPI api = parser.read("src/test/resources/swagger.json", null, new ParseOptions());
		assertThat(api).isNotNull();
		assertThat(api.getPaths()).containsKeys("/generated", "/manual");
		// System.err.println(mapper.writeValueAsString(api));
		SwaggerParseResult result = parser.readContents(mapper.writeValueAsString(api), null, new ParseOptions());
		// Swagger parser adds invalid attributes (or they are not @JsonIgnored where
		// they should be)
		assertThat(result.getMessages()).containsExactly("attribute extensions is unexpected");
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
