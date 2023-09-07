package com.example.aggregator;

import java.net.URI;
import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.example.aggregator.OpenApiAggregatorSpecs.Spec;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;

@Component
public class OpenApiAggregator {

	private final ObjectMapper mapper;
	private OpenApiAggregatorSpecs specs;
	private OpenAPI base;

	OpenApiAggregator(OpenApiAggregatorSpecs specs, ObjectMapper mapper, OpenAPI base) {
		this.specs = specs;
		this.mapper = mapper;
		this.base = base;
	}

	public OpenAPI aggregate() {
		OpenAPI api = new OpenAPI();
		merge(api, base);
		api.setInfo(base.getInfo());
		if (base.getTags()!=null) {
			api.setTags(base.getTags());
		}
		for (Spec spec : specs.getSpecs()) {
			OpenAPI item;
			try {
				// Blocking...
				item = mapper.readValue(URI.create(spec.uri()).toURL(), OpenAPI.class);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			item.paths(spec.paths(item.getPaths()));
			merge(api, item);
		}
		return api;
	}

	private void merge(OpenAPI api, OpenAPI item) {
		Paths paths = item.getPaths();
		if (api.getPaths() == null) {
			api.paths(new Paths());
		}
		for (String path : paths.keySet()) {
			api.getPaths().addPathItem(path, paths.get(path));
		}
		Components components = item.getComponents();
		if (components != null && components.getSchemas() != null) {
			if (api.getComponents() == null) {
				api.components(new Components());
			}
			if (api.getComponents().getSchemas() == null) {
				api.getComponents().setSchemas(new HashMap<>());
			}
			for (String schema : components.getSchemas().keySet()) {
				api.getComponents().getSchemas().put(schema, components.getSchemas().get(schema));
			}
		}
	}

}
