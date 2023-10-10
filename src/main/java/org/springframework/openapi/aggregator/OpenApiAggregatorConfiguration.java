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

import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.service.OpenAPIService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Autoconfiguration for the OpenAPI aggregator.
 */
@Configuration
@ConditionalOnBean(OpenApiAggregatorSpecs.class)
@AutoConfigureBefore(SpringDocConfiguration.class)
@Import(SpringDocSpecConfiguration.class)
@EnableConfigurationProperties(OpenApiAggregatorProperties.class)
public class OpenApiAggregatorConfiguration {

	/**
	 * Create a new {@link OpenApiAggregator} instance.
	 * @param specs the specs to use
	 * @param properties the configuration, e.g. for common info
	 * @return an aggregator
	 */
	@Bean
	public OpenApiAggregator openApiAggregator(OpenApiAggregatorSpecs specs, OpenApiAggregatorProperties properties) {
		return new OpenApiAggregator(specs, properties.getBase());
	}

	/**
	 * Create a new {@link AggregatorEndpoint} instance to expose the aggregated spec over
	 * HTTP.
	 * @param aggregator the aggregator to use
	 * @return an endpoint that can be used in WebMVC or WebFlux
	 */
	@Bean
	@ConditionalOnWebApplication
	@ConditionalOnMissingBean(type = "org.springdoc.core.service.OpenAPIService")
	public AggregatorEndpoint aggregatorEndpoint(OpenApiAggregator aggregator) {
		return new AggregatorEndpoint(aggregator);
	}

}

@Configuration
@ConditionalOnClass(OpenAPIService.class)
class SpringDocSpecConfiguration {

	/**
	 * Create a new {@link OpenAPI} instance to inject into the SpringDoc spec generator.
	 * @param aggregator the aggregator to use
	 * @return an OpenAPI spec generated from the endpoints in this application
	 */
	@Bean
	OpenAPI openAPIBaseSpec(OpenApiAggregator aggregator) {
		return aggregator.aggregate();
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