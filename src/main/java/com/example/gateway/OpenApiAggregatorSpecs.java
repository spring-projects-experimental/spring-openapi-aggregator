package com.example.gateway;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import io.swagger.v3.oas.models.Paths;

public class OpenApiAggregatorSpecs {

	record Spec(String uri, Function<Paths, Paths> paths) {

		Spec(String uri) {
			this(uri, Function.identity());
		}

		public Paths paths(Paths paths) {
			return paths().apply(paths);
		}

		public Spec prefix(String prefix) {
			return new Spec(uri(), paths().andThen(paths -> {
				Paths result = new Paths();
				for (String path : paths.keySet()) {
					result.addPathItem(prefix + path, paths.get(path));
				}
				return result;
			}));
		}

		public Spec replace(String pattern, String replacement) {
			return new Spec(uri(), paths().andThen(paths -> {
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
