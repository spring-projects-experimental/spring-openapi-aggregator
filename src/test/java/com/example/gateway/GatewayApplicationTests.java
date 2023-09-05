package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.models.OpenAPI;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTests {

	@Autowired
	private TestRestTemplate rest;

	@Test
	void contextLoads() {
		ResponseEntity<OpenAPI> response = rest.getForEntity("/v3/api-docs", OpenAPI.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getPaths()).containsKeys("/dates/AvailableCountries", "/wizards/Elixirs");
	}

}
