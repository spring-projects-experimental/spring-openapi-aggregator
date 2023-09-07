package com.example.aggregator;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.example.aggregator.OpenApiAggregatorSpecs.Spec;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;

@Component
public class OpenApiAggregator {

	private final ObjectMapper mapper;
	private final OpenApiAggregatorSpecs specs;
	private final OpenAPI base;

	public OpenApiAggregator(OpenApiAggregatorSpecs specs, ObjectMapper mapper, OpenAPI base) {
		this.specs = specs;
		this.mapper = mapper;
		this.base = base;
	}

	public OpenAPI aggregate() {
		OpenAPI api = new OpenAPI();
		merge(api, base);
		api.setInfo(base.getInfo());
		if (base.getTags() != null) {
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
		Components target = api.getComponents();
		if (target == null) {
			target = new Components();
			api.components(target);
		}
		for (String path : paths.keySet()) {
			api.getPaths().addPathItem(path, paths.get(path));
		}
		merge(paths.getExtensions(), api.getPaths()::getExtensions,  api.getPaths()::setExtensions);
		Components source = item.getComponents();
		if (source != null) {
			merge(source.getCallbacks(), target::getCallbacks, target::setCallbacks);
			merge(source.getExamples(), target::getExamples, target::setExamples);
			merge(source.getExtensions(), target::getExtensions, target::setExtensions);
			merge(source.getHeaders(), target::getHeaders, target::setHeaders);
			merge(source.getLinks(), target::getLinks, target::setLinks);
			merge(source.getParameters(), target::getParameters, target::setParameters);
			merge(source.getPathItems(), target::getPathItems, target::setPathItems);
			merge(source.getRequestBodies(), target::getRequestBodies, target::setRequestBodies);
			merge(source.getResponses(), target::getResponses, target::setResponses);
			merge(source.getSchemas(), target::getSchemas, target::setSchemas);
			merge(source.getSecuritySchemes(), target::getSecuritySchemes, target::setSecuritySchemes);
		}
	}

	private <T> void merge(Map<String, T> source, Supplier<Map<String, T>> getter, Consumer<Map<String, T>> setter) {
		if (source != null) {
			if (getter.get() == null) {
				setter.accept(new HashMap<>());
			}
			getter.get().putAll(source);
		}
	}

}
