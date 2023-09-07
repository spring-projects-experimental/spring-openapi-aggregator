package com.example.aggregator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.example.aggregator.OpenApiAggregatorSpecs.Spec;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;

@Component
public class OpenApiAggregator {

	private final OpenApiAggregatorSpecs specs;
	private final OpenAPI base;

	public OpenApiAggregator(OpenApiAggregatorSpecs specs, OpenAPI base) {
		this.specs = specs;
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
				OpenAPIV3Parser parser = new OpenAPIV3Parser();
				item = parser.read(spec.resource().getURL().toString());
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
		if (paths != null) {
			if (api.getPaths() == null) {
				api.paths(new Paths());
			}
			for (String path : paths.keySet()) {
				api.getPaths().addPathItem(path, paths.get(path));
			}
			merge(paths.getExtensions(), api.getPaths()::getExtensions, api.getPaths()::setExtensions);
		}
		Components target = api.getComponents();
		if (target == null) {
			target = new Components();
			api.components(target);
		}
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
