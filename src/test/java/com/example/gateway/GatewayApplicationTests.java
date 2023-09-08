package com.example.gateway;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.openapi.base.info.description=Gateway API In Test")
class GatewayApplicationTests {

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
		assertThat(api.getInfo().getDescription()).isEqualTo("Gateway API In Test");
	}

}
