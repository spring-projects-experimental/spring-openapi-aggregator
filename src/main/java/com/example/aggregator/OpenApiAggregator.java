package com.example.aggregator;

import java.net.URI;
import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.example.aggregator.OpenApiAggregatorSpecs.Spec;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;

@Component
public class OpenApiAggregator {

	private final ObjectMapper mapper;
	private OpenApiAggregatorSpecs specs;

	OpenApiAggregator(OpenApiAggregatorSpecs specs, ObjectMapper mapper) {
		this.specs = specs;
		this.mapper = mapper;
	}

	public OpenAPI aggregate() {
		OpenAPI api = new OpenAPI();
		api.paths(new Paths());
		api.components(new Components());
		api.info(new Info()
				.title("Gateway API")
				.description("Gateway API")
				.version("1.0.0"));
		for (Spec spec : specs.getSpecs()) {
			OpenAPI item;
			try {
				// Blocking...
				item = mapper.readValue(URI.create(spec.uri()).toURL(), OpenAPI.class);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			Paths paths = spec.paths(item.getPaths());
			for (String path : paths.keySet()) {
				api.getPaths().addPathItem(path, paths.get(path));
			}
			Components components = item.getComponents();
			if (components != null && components.getSchemas() != null && api.getComponents().getSchemas() == null) {
				api.getComponents().setSchemas(new HashMap<>());
			}
			for (String schema : components.getSchemas().keySet()) {
				api.getComponents().getSchemas().put(schema, components.getSchemas().get(schema));
			}
		}
		return api;
	}

}
