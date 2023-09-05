package com.example.gateway;

import java.util.HashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.gateway.OpenApiAggregatorSpecs.Spec;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OpenApiAggregator {

	private final WebClient rest;
	private OpenApiAggregatorSpecs specs;

	OpenApiAggregator(OpenApiAggregatorSpecs specs, WebClient.Builder rest) {
		this.specs = specs;
		this.rest = rest.build();
	}

	public Mono<OpenAPI> aggregate() {
		OpenAPI api = new OpenAPI();
		api.paths(new Paths());
		api.components(new Components());
		api.info(new Info()
				.title("Gateway API")
				.description("Gateway API")
				.version("1.0.0"));
		Flux<Spec> flux = Flux.fromIterable(specs.getSpecs());
		return flux.flatMap(spec -> rest.get()
				.uri(spec.uri())
				.retrieve()
				.bodyToMono(OpenAPI.class)
				.map(item -> {
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
					return api;
				})).then(Mono.just(api));
	}

}
