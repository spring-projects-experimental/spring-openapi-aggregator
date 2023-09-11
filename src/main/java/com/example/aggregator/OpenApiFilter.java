package com.example.aggregator;

import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;

public interface OpenApiFilter {

	OpenAPI filter(OpenAPI api);

	default Map<String, String> getPathMappings() {
		return Map.of();
	}

	default Map<String, String> getOperationMappings() {
		return Map.of();
	}

	default Map<String, String> getSchemaMappings() {
		return Map.of();
	}

}
