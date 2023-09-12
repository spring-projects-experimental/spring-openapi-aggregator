/*
 * Copyright 2023-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.openapi.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.openapi.aggregator.OpenApiAggregator;
import org.springframework.openapi.aggregator.OpenApiAggregatorSpecs;
import org.springframework.openapi.aggregator.OpenApiAggregatorSpecs.Spec;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
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
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("openapi.json"))), base);
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
	public void testProcessor() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(new OpenApiAggregatorSpecs().processor((api, items) -> {
			api.setInfo(new Info().title("Test").version("v0"));
			return api;
		}), base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getInfo().getTitle()).isEqualTo("Test");
	}

	@Test
	public void testRequestBody() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("posts.json")).schemaPrefix("V1")),
				base);
		OpenAPI api = aggregator.aggregate();
		// System.err.println(mapper.writeValueAsString(api));
		assertThat(api.getComponents().getSchemas().get("V1Model")).isNotNull();
		assertThat(api.getPaths()
			.get("/manual")
			.getPost()
			.getResponses()
			.get("200")
			.getContent()
			.get("application/json")
			.getSchema()
			.get$ref()).isEqualTo("#/components/schemas/V1Model");
		assertThat(api.getPaths()
			.get("/manual")
			.getPost()
			.getRequestBody()
			.getContent()
			.get("application/json")
			.getSchema()
			.get$ref()).isEqualTo("#/components/schemas/V1Model");
		assertThat(api.getComponents()
			.getRequestBodies()
			.get("Model")
			.getContent()
			.get("application/json")
			.getSchema()
			.get$ref()).isEqualTo("#/components/schemas/V1Model");
	}

	@Test
	public void testArraysInSchemaProperties() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("arrays.json")).schemaPrefix("V1")),
				base);
		OpenAPI api = aggregator.aggregate();
		// System.err.println(mapper.writeValueAsString(api));
		assertThat(api.getComponents().getSchemas().get("V1Models")).isNotNull();
		Schema<?> schema = (Schema<?>) api.getComponents().getSchemas().get("V1Models").getProperties().get("values");
		assertThat(schema.getItems().get$ref()).isEqualTo("#/components/schemas/V1Model");
	}

	@Test
	public void testSchemaPrefix() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("schemas.json")).schemaPrefix("V1")),
				base);
		OpenAPI api = aggregator.aggregate();
		// System.err.println(mapper.writeValueAsString(api));
		assertThat(api.getComponents().getSchemas().get("V1Model")).isNotNull();
		assertThat(api.getPaths()
			.get("/manual")
			.getGet()
			.getResponses()
			.get("200")
			.getContent()
			.get("application/json")
			.getSchema()
			.get$ref()).isEqualTo("#/components/schemas/V1Model");
	}

	@Test
	public void testSchemaPrefixArray() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("arrays.json")).schemaPrefix("V1")),
				base);
		OpenAPI api = aggregator.aggregate();
		// System.err.println(mapper.writeValueAsString(api));
		assertThat(api.getComponents().getSchemas().get("V1Model")).isNotNull();
		assertThat(api.getPaths()
			.get("/manual")
			.getPost()
			.getResponses()
			.get("200")
			.getContent()
			.get("application/json")
			.getSchema()
			.getItems()
			.get$ref()).isEqualTo("#/components/schemas/V1Model");
	}

	@Test
	public void testSchemaPrefixResponses() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("schemas.json")).schemaPrefix("V1")),
				base);
		OpenAPI api = aggregator.aggregate();
		// System.err.println(mapper.writeValueAsString(api));
		assertThat(api.getComponents().getSchemas().get("V1Error")).isNotNull();
		assertThat(api.getComponents()
			.getResponses()
			.get("NotFound")
			.getContent()
			.get("application/json")
			.getSchema()
			.get$ref()).isEqualTo("#/components/schemas/V1Error");
	}

	@Test
	public void testOperationPrefixComponentsLinksOperationId() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("links.json")).operationPrefix("v1")),
				base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths().get("/manual").getGet().getResponses().get("200").getLinks().get("message").get$ref())
			.isEqualTo("#/components/links/message");
		assertThat(api.getComponents().getLinks().get("message").getOperationId()).isEqualTo("v1message");
	}

	@Test
	public void testOperationPrefixComponentsLinksOperationRef() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(
				new OpenApiAggregatorSpecs().spec(new Spec(new ClassPathResource("links.json")).prefix("/v1")), base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()
			.get("/v1/manual")
			.getGet()
			.getResponses()
			.get("200")
			.getLinks()
			.get("messageRef")
			.get$ref()).isEqualTo("#/components/links/messageRef");
		assertThat(api.getComponents().getLinks().get("messageRef").getOperationRef())
			.isEqualTo("#/paths/~1v1~1manual/get");
	}

	@Test
	public void testTwoVersions() throws Exception {
		OpenApiAggregator aggregator = new OpenApiAggregator(new OpenApiAggregatorSpecs().spec(
				new Spec(new ClassPathResource("openapi.json")).prefix("/v1").operationPrefix("V1").schemaPrefix("V1"))
			.spec(new Spec(new ClassPathResource("openapi.json")).prefix("/v2")), base);
		OpenAPI api = aggregator.aggregate();
		assertThat(api.getPaths()).containsKeys("/v1/generated", "/v1/manual");
		assertThat(api.getPaths()).containsKeys("/v2/generated", "/v2/manual");
		assertThat(api.getPaths().get("/v1/manual").getGet().getOperationId()).startsWith("V1");
		assertThat(api.getPaths()
			.get("/v1/manual")
			.getGet()
			.getResponses()
			.get("200")
			.getContent()
			.get("application/json")
			.getSchema()
			.get$ref()).isEqualTo("#/components/schemas/V1Model");
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		SwaggerParseResult result = parser.readContents(mapper.writeValueAsString(api), null, new ParseOptions());
		assertThat(result.getMessages()).isEmpty();
	}

}
