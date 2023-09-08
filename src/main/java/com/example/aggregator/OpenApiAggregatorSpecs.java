package com.example.aggregator;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;

public class OpenApiAggregatorSpecs {

	public record Spec(Resource resource, Function<Paths, Paths> paths) {

		public Spec(String uri) {
			this(UrlResource.from(uri), Function.identity());
		}

		public Spec(Resource resource) {
			this(resource, Function.identity());
		}

		public Paths paths(Paths paths) {
			return paths().apply(paths);
		}

		public Spec prefix(String prefix) {
			return new Spec(resource(), paths().andThen(paths -> {
				Paths result = new Paths();
				for (String path : paths.keySet()) {
					result.addPathItem(prefix + path, paths.get(path));
				}
				return result;
			}));
		}

		public Spec operationPrefix(String prefix) {
			return new Spec(resource(), paths().andThen(paths -> {
				for (String path : paths.keySet()) {
					for (Operation operation : paths.get(path).readOperations()) {
						if (operation.getOperationId() != null) {
							operation.setOperationId(prefix + operation.getOperationId());
						}
					}
				}
				return paths;
			}));
		}

		public Spec replace(String pattern, String replacement) {
			return new Spec(resource(), paths().andThen(paths -> {
				Paths result = new Paths();
				for (String path : paths.keySet()) {
					if (path.contains(pattern)) {
						result.addPathItem(path.replace(pattern, replacement), paths.get(path));
					}
				}
				return result;
			}));
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
