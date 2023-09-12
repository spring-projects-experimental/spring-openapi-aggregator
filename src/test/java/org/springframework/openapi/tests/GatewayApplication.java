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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.openapi.aggregator.OpenApiAggregatorSpecs;
import org.springframework.openapi.aggregator.OpenApiAggregatorSpecs.Spec;

/**
 * Gateway application to use for testing, aggregates 2 public APIs from the internet and
 * serves them up through a gateway. Run from the IDE or command line with
 * <code>./mvnw spring-boot:test-run</code>.
 */
@SpringBootApplication
public class GatewayApplication {

	private String dates = "https://date.nager.at";

	private String wizards = "https://wizard-world-api.herokuapp.com";

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return rlb.routes()
			.route(r -> r.path("/dates/**").filters(f -> f.stripPrefix(1).prefixPath("/api/v3")).uri(dates))
			.route(r -> r.path("/wizards/**").filters(f -> f.stripPrefix(1)).uri(wizards))
			.build();
	}

	@Bean
	OpenApiAggregatorSpecs specs() {
		return new OpenApiAggregatorSpecs()
			.spec(new Spec(dates + "/swagger/v3/swagger.json").replace("/api/v3", "/dates")
				.schemas(schema -> schema.endsWith("Dto") ? schema.substring(0, schema.length() - 3) : schema))
			.spec(new Spec(wizards + "/swagger/v1/swagger.json").prefix("/wizards")
				.schemas(schema -> schema.endsWith("Dto") ? schema.substring(0, schema.length() - 3) : schema));
	}

}