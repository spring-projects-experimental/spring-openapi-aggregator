package org.springframework.openapi.aggregator;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;

@ConfigurationProperties(prefix = "spring.openapi")
public class OpenApiAggregatorProperties {

	private OpenAPI base = new OpenAPI();

	public OpenApiAggregatorProperties() {
		base.paths(new Paths());
		base.components(new Components());
		base.info(new Info().title("Gateway API").description("Gateway API").version("1.0.0"));
	}

	public OpenAPI getBase() {
		return base;
	}

}
