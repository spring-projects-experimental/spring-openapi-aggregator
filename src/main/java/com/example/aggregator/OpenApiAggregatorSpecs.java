package com.example.aggregator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;

public class OpenApiAggregatorSpecs {

	public record Spec(Resource resource, Function<String, String> paths, Function<String, String> operations) {

		public Spec(String uri) {
			this(UrlResource.from(uri), Function.identity(), Function.identity());
		}

		public Spec(Resource resource) {
			this(resource, Function.identity(), Function.identity());
		}

		public SpecProcessor processor() {
			return new SimpleSpecProcessor(this);
		}

		public Spec prefix(String prefix) {
			return new Spec(resource(), paths().andThen(path -> {
				return prefix + path;
			}), operations());
		}

		public Spec replace(String pattern, String replacement) {
			return new Spec(resource(), paths().andThen(path -> {
				if (path.contains(pattern)) {
					return path.replace(pattern, replacement);
				}
				return path;
			}), operations());
		}

		public Spec operationPrefix(String prefix) {
			return new Spec(resource(), paths(), operations().andThen(operation -> {
				if (operation != null) {
					return prefix + operation;
				}
				return operation;
			}));
		}

	}

	interface SpecProcessor {
		OpenAPI apply(OpenAPI source);
	}

	private static class SimpleSpecProcessor implements SpecProcessor {

		private final Map<String, String> pathReplacements = new HashMap<>();
		private final Map<String, String> operationReplacements = new HashMap<>();
		private final Spec spec;

		private SimpleSpecProcessor(Spec spec) {
			this.spec = spec;
		}

		@Override
		public OpenAPI apply(OpenAPI source) {
			Paths paths = new Paths();
			for (String path : source.getPaths().keySet()) {
				String newPath = spec.paths().apply(path);
				if (newPath != null) {
					if (!newPath.equals(path)) {
						pathReplacements.put(path, newPath);
					}
					paths.addPathItem(newPath, source.getPaths().get(path));
				}
				for (Operation operation : source.getPaths().get(path).readOperations()) {
					if (operation.getOperationId() != null) {
						String newOperation = spec.operations().apply(operation.getOperationId());
						if (newOperation != null) {
							if (!newOperation.equals(operation.getOperationId())) {
								operationReplacements.put(operation.getOperationId(), newOperation);
							}
							operation.setOperationId(newOperation);
						}
					}
				}
			}
			source.setPaths(paths);
			for (String path : source.getPaths().keySet()) {
				for (Operation operation : source.getPaths().get(path).readOperations()) {
					if (operation.getResponses() != null) {
						for (Object key : operation.getResponses().keySet()) {
							ApiResponse response = operation.getResponses().get(key);
							if (response.getLinks() != null) {
								for (String link : response.getLinks().keySet()) {
									if (response.getLinks().get(link).getOperationId() != null) {
										String newOperation = operationReplacements
												.get(response.getLinks().get(link).getOperationId());
										if (newOperation != null) {
											response.getLinks().get(link).setOperationId(newOperation);
										}
									}
								}
							}
						}
					}
				}
			}
			if (source.getComponents() != null && source.getComponents().getLinks() != null) {
				for (Object key : source.getComponents().getLinks().keySet()) {
					if (source.getComponents().getLinks().get(key).getOperationId() != null) {
						String newOperation = operationReplacements
								.get(source.getComponents().getLinks().get(key).getOperationId());
						if (newOperation != null) {
							source.getComponents().getLinks().get(key).setOperationId(newOperation);
						}
					}
				}
			}
			return source;
		}
	}

	private Set<Spec> specs = new HashSet<>();

	public Set<Spec> getSpecs() {
		return this.specs;
	}

	public void setSpecs(Set<Spec> specs) {
		this.specs = specs;
	}

	public OpenApiAggregatorSpecs spec(Spec spec) {
		this.specs.add(spec);
		return this;
	}
}
