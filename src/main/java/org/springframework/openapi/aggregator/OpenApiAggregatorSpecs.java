/*
 * Copyright 2023-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.openapi.aggregator;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

public class OpenApiAggregatorSpecs {

	public record Spec(Resource resource, Function<OpenAPI, OpenAPI> filter) {

		public Spec(String uri) {
			this(UrlResource.from(uri), api -> api);
		}

		public Spec(Resource resource) {
			this(resource, api -> api);
		}

		public Spec filter(Function<OpenAPI, OpenAPI> filter) {
			return new Spec(resource(), filter().andThen(filter));
		}

		public Spec paths(Function<String, String> paths) {
			return filter(pathFilter(paths));
		}

		public Spec operations(Function<String, String> operations) {
			return filter(operationFilter(operations));
		}

		public Spec schemas(Function<String, String> schemas) {
			return filter(schemaFilter(schemas));
		}

		public Spec prefix(String prefix) {
			return paths(path -> prefix + path);
		}

		public Spec replace(String pattern, String replacement) {
			return paths(path -> {
				if (path.contains(pattern)) {
					return path.replace(pattern, replacement);
				}
				return path;
			});
		}

		public Spec operationPrefix(String prefix) {
			return operations(operation -> {
				if (operation != null) {
					return prefix + operation;
				}
				return operation;
			});
		}

		public Spec schemaPrefix(String prefix) {
			return schemas(schema -> prefix + schema);
		}

		private static Function<OpenAPI, OpenAPI> pathFilter(Function<String, String> paths) {
			return new SimpleSpecProcessor(paths, Function.identity(), Function.identity());
		}

		private static Function<OpenAPI, OpenAPI> operationFilter(Function<String, String> operations) {
			return new SimpleSpecProcessor(Function.identity(), operations, Function.identity());
		}

		private static Function<OpenAPI, OpenAPI> schemaFilter(Function<String, String> schemas) {
			return new SimpleSpecProcessor(Function.identity(), Function.identity(), schemas);
		}

	}

	private Set<Spec> specs = new LinkedHashSet<>();

	private Function<OpenAPI, OpenAPI> filter = Function.identity();

	private BiFunction<OpenAPI, Set<OpenAPI>, OpenAPI> processor = (api, items) -> api;

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

	public Function<OpenAPI, OpenAPI> getFilter() {
		return filter;
	}

	public BiFunction<OpenAPI, Set<OpenAPI>, OpenAPI> getProcessor() {
		return processor;
	}

	public OpenApiAggregatorSpecs filter(Function<OpenAPI, OpenAPI> filter) {
		this.filter = this.filter.andThen(filter);
		return this;
	}

	private static String replacePath(String operationRef, String newPath) {
		String path = operationRef;
		if (path.contains("~1")) {
			path = path.substring(path.indexOf("~1"));
		}
		if (path.contains("/")) {
			path = path.substring(0, path.indexOf("/"));
		}
		return operationRef.replace(path, newPath.replace("/", "~1"));
	}

	private static String extractPath(String operationRef) {
		String path = operationRef;
		if (path.contains("~1")) {
			path = path.substring(path.indexOf("~1"));
		}
		if (path.contains("/")) {
			path = path.substring(0, path.indexOf("/"));
		}
		return path.replace("~1", "/");
	}

	private static String schemaPath(String schema) {
		return "#/components/schemas/" + schema.replace("/", "~1");
	}

	private static String modelName(String schema) {
		return schema.replace("#/components/schemas/", "").replace("~1", "/");
	}

	private static class SimpleSpecProcessor implements Function<OpenAPI, OpenAPI> {

		private final Map<String, String> pathReplacements = new HashMap<>();

		private final Map<String, String> operationReplacements = new HashMap<>();

		private final Map<String, String> schemaReplacements = new HashMap<>();

		private final Function<String, String> paths;

		private final Function<String, String> operations;

		private final Function<String, String> schemas;

		public SimpleSpecProcessor(Function<String, String> paths, Function<String, String> operations,
				Function<String, String> schemas) {
			this.paths = paths;
			this.operations = operations;
			this.schemas = schemas;
		}

		@Override
		public OpenAPI apply(OpenAPI source) {
			source.setPaths(transformPaths(source.getPaths()));
			source.setComponents(transformComponents(source.getComponents()));
			for (String path : source.getPaths().keySet()) {
				for (Operation operation : source.getPaths().get(path).readOperations()) {
					RequestBody body = operation.getRequestBody();
					if (body != null) {
						if (body.getContent() != null) {
							for (String type : body.getContent().keySet()) {
								Schema<?> schema = body.getContent().get(type).getSchema();
								transformSchema(schema);
							}
						}
					}
					if (operation.getResponses() != null) {
						for (String key : operation.getResponses().keySet()) {
							ApiResponse response = operation.getResponses().get(key);
							transformResponse(response);
						}
					}
				}
			}
			if (source.getComponents() != null && source.getComponents().getLinks() != null) {
				for (String key : source.getComponents().getLinks().keySet()) {
					Link link = source.getComponents().getLinks().get(key);
					transformLink(link);
				}
			}
			if (source.getComponents() != null && source.getComponents().getSchemas() != null) {
				for (String key : source.getComponents().getSchemas().keySet()) {
					Schema<?> schema = source.getComponents().getSchemas().get(key);
					transformSchema(schema);
				}
			}
			if (source.getComponents() != null && source.getComponents().getRequestBodies() != null) {
				for (String key : source.getComponents().getRequestBodies().keySet()) {
					RequestBody body = source.getComponents().getRequestBodies().get(key);
					if (body != null) {
						if (body.getContent() != null) {
							for (String type : body.getContent().keySet()) {
								Schema<?> schema = body.getContent().get(type).getSchema();
								transformSchema(schema);
							}
						}
					}
				}
			}
			if (source.getComponents() != null && source.getComponents().getResponses() != null) {
				for (String key : source.getComponents().getResponses().keySet()) {
					ApiResponse response = source.getComponents().getResponses().get(key);
					transformResponse(response);
				}
			}
			return source;
		}

		private Components transformComponents(Components source) {
			if (source != null && source.getSchemas() != null) {
				@SuppressWarnings("rawtypes")
				Map<String, Schema> schemas = new HashMap<>(source.getSchemas());
				for (String schema : schemas.keySet()) {
					String newSchema = this.schemas.apply(schema);
					if (newSchema != null && !newSchema.equals(schema)) {
						schemaReplacements.put(schema, newSchema);
						Schema<?> value = source.getSchemas().remove(schema);
						source.getSchemas().put(newSchema, value);
					}
				}
			}
			return source;
		}

		private Paths transformPaths(Paths source) {
			Paths paths = new Paths();
			for (String path : source.keySet()) {
				String newPath = this.paths.apply(path);
				if (newPath != null) {
					if (!newPath.equals(path)) {
						pathReplacements.put(path, newPath);
					}
					paths.addPathItem(newPath, source.get(path));
				}
				for (Operation operation : source.get(path).readOperations()) {
					if (operation.getOperationId() != null) {
						String newOperation = this.operations.apply(operation.getOperationId());
						if (newOperation != null) {
							if (!newOperation.equals(operation.getOperationId())) {
								operationReplacements.put(operation.getOperationId(), newOperation);
							}
							operation.setOperationId(newOperation);
						}
					}
				}
			}
			return paths;
		}

		private void transformLink(Link link) {
			if (link.getOperationId() != null) {
				String newOperation = operationReplacements.get(link.getOperationId());
				if (newOperation != null) {
					link.setOperationId(newOperation);
				}
			}
			if (link.getOperationRef() != null) {
				String path = extractPath(link.getOperationRef());
				if (pathReplacements.containsKey(path)) {
					link.setOperationRef(replacePath(link.getOperationRef(), pathReplacements.get(path)));
				}
			}
		}

		private void transformSchema(Schema<?> schema) {
			if (schema != null) {
				if (schema.get$ref() != null) {
					String newSchema = schemaReplacements.get(modelName(schema.get$ref()));
					if (newSchema != null) {
						schema.set$ref(schemaPath(newSchema));
					}
				}
				if (schema.getProperties() != null) {
					for (String property : schema.getProperties().keySet()) {
						Schema<?> propertySchema = schema.getProperties().get(property);
						transformSchema(propertySchema);
					}
				}
				if (schema.getItems() != null) {
					Schema<?> itemSchema = schema.getItems();
					transformSchema(itemSchema);
				}
			}
		}

		private void transformResponse(ApiResponse response) {
			if (response.getLinks() != null) {
				for (String link : response.getLinks().keySet()) {
					if (response.getLinks().get(link).getOperationId() != null) {
						String newOperation = operationReplacements.get(response.getLinks().get(link).getOperationId());
						if (newOperation != null) {
							response.getLinks().get(link).setOperationId(newOperation);
						}
					}
				}
			}
			if (response.getContent() != null) {
				for (String type : response.getContent().keySet()) {
					Schema<?> schema = response.getContent().get(type).getSchema();
					transformSchema(schema);
				}
			}
		}

	}

}
