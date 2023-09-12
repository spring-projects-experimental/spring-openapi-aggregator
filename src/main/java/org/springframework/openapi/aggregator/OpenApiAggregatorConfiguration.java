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
package org.springframework.openapi.aggregator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;

/**
 * Autoconfiguration for the OpenAPI aggregator.
 */
@Configuration
@ConditionalOnBean(OpenApiAggregatorSpecs.class)
public class OpenApiAggregatorConfiguration {

	/**
	 * Create a new {@link OpenApiAggregator} instance.
	 * @param specs the specs to use
	 * @param base the base to merge with, e.g. for common info
	 * @return an aggregator
	 */
	@Bean
	public OpenApiAggregator openApiAggregator(OpenApiAggregatorSpecs specs,
			@Qualifier("spring.openapi.base") OpenAPI base) {
		return new OpenApiAggregator(specs, base);
	}

	/**
	 * Create a new {@link OpenAPI} instance and make it configurable via Spring Boot.
	 * @return a base instance
	 */
	@Bean("spring.openapi.base")
	@ConfigurationProperties("spring.openapi.base")
	public OpenAPI base() {
		OpenAPI api = new OpenAPI();
		api.paths(new Paths());
		api.components(new Components());
		api.info(new Info().title("Gateway API").description("Gateway API").version("1.0.0"));
		return api;
	}

	/**
	 * Create a new {@link AggregatorEndpoint} instance to expose the aggregated spec over
	 * HTTP.
	 * @param aggregator the aggregator to use
	 * @return an endpoint that can be used in WebMVC or WebFlux
	 */
	@Bean
	@ConditionalOnWebApplication
	public AggregatorEndpoint aggregatorEndpoint(OpenApiAggregator aggregator) {
		return new AggregatorEndpoint(aggregator);
	}

}

@RestController
class AggregatorEndpoint implements InitializingBean {

	private final OpenApiAggregator aggregator;

	private OpenAPI api;

	private final ObjectMapper mapper = Json.mapper();

	public AggregatorEndpoint(OpenApiAggregator aggregator) {
		this.aggregator = aggregator;
		mapper.setDefaultPropertyInclusion(
				JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_DEFAULT));
	}

	@GetMapping(path = "${spring.openapi.aggregator.path:/v3/api-docs}", produces = { "application/json" })
	public String api() {
		try {
			return mapper.writeValueAsString(api);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.api = aggregator.aggregate();
	}

}