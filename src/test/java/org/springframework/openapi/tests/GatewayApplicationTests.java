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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.openapi.base.info.description=Gateway API In Test")
public class GatewayApplicationTests {

	private ObjectMapper mapper = Json.mapper();

	{
		mapper.setDefaultPropertyInclusion(
				JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_DEFAULT));
	}

	@Autowired
	private TestRestTemplate rest;

	@Test
	void contextLoads() throws Exception {
		ResponseEntity<String> response = rest.getForEntity("/v3/api-docs", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).doesNotContain("\"style\": \"form\"");
		OpenAPI api = mapper.readValue(response.getBody(), OpenAPI.class);
		assertThat(api.getPaths()).containsKeys("/dates/AvailableCountries", "/wizards/Elixirs");
		assertThat(api.getPaths()).containsKeys("/actuator/health");
		assertThat(api.getInfo().getDescription()).isEqualTo("Gateway API In Test");
	}

}
