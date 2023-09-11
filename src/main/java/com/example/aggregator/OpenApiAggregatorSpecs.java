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
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

public class OpenApiAggregatorSpecs {

	public record Spec(Resource resource, Function<String, String> paths, Function<String, String> operations,
			Function<String, String> schemas) {

		public Spec(String uri) {
			this(UrlResource.from(uri), Function.identity(), Function.identity(), Function.identity());
		}

		public Spec(Resource resource) {
			this(resource, Function.identity(), Function.identity(), Function.identity());
		}

		public SpecProcessor processor() {
			return new SimpleSpecProcessor(this);
		}

		public Spec paths(Function<String, String> paths) {
			return new Spec(resource(), paths().andThen(paths), operations(), schemas());
		}

		public Spec operations(Function<String, String> operations) {
			return new Spec(resource(), paths(), operations().andThen(operations), schemas());
		}

		public Spec schemas(Function<String, String> schemas) {
			return new Spec(resource(), paths(), operations(), schemas().andThen(schemas));
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

	}

	interface SpecProcessor {
		OpenAPI apply(OpenAPI source);
	}

	private static class SimpleSpecProcessor implements SpecProcessor {

		private final Map<String, String> pathReplacements = new HashMap<>();
		private final Map<String, String> operationReplacements = new HashMap<>();
		private final Map<String, String> schemaReplacements = new HashMap<>();
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
			if (source.getComponents() != null && source.getComponents().getSchemas() != null) {
				@SuppressWarnings("rawtypes")
				Map<String, Schema> schemas = new HashMap<>(source.getComponents().getSchemas());
				for (String schema : schemas.keySet()) {
					String newSchema = spec.schemas().apply(schema);
					if (newSchema != null) {
						if (!newSchema.equals(schema)) {
							schemaReplacements.put(schema, newSchema);
						}
						Schema<?> value = source.getComponents().getSchemas().remove(schema);
						source.getComponents().getSchemas().put(newSchema, value);
					}

				}
			}
			for (String path : source.getPaths().keySet()) {
				for (Operation operation : source.getPaths().get(path).readOperations()) {
					RequestBody body = operation.getRequestBody();
					if (body != null) {
						if (body.getContent() != null) {
							for (String type : body.getContent().keySet()) {
								Schema<?> schema = body.getContent().get(type).getSchema();
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
											if (propertySchema.get$ref() != null) {
												String newSchema = schemaReplacements.get(propertySchema.get$ref());
												if (newSchema != null) {
													propertySchema.set$ref(newSchema);
												}
											}
										}
									}
								}
							}
						}
					}
					if (operation.getResponses() != null) {
						for (String key : operation.getResponses().keySet()) {
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
							if (response.getContent() != null) {
								for (String type : response.getContent().keySet()) {
									Schema<?> schema = response.getContent().get(type).getSchema();
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
												if (propertySchema.get$ref() != null) {
													String newSchema = schemaReplacements.get(propertySchema.get$ref());
													if (newSchema != null) {
														propertySchema.set$ref(newSchema);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if (source.getComponents() != null && source.getComponents().getLinks() != null) {
				for (String key : source.getComponents().getLinks().keySet()) {
					Link link = source.getComponents().getLinks().get(key);
					if (link.getOperationId() != null) {
						String newOperation = operationReplacements
								.get(link.getOperationId());
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
			}
			if (source.getComponents() != null && source.getComponents().getRequestBodies() != null) {
				for (String key : source.getComponents().getRequestBodies().keySet()) {
					RequestBody body = source.getComponents().getRequestBodies().get(key);
					if (body != null) {
						if (body.getContent() != null) {
							for (String type : body.getContent().keySet()) {
								Schema<?> schema = body.getContent().get(type).getSchema();
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
											if (propertySchema.get$ref() != null) {
												String newSchema = schemaReplacements.get(propertySchema.get$ref());
												if (newSchema != null) {
													propertySchema.set$ref(newSchema);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if (source.getComponents() != null && source.getComponents().getResponses() != null) {
				for (String key : source.getComponents().getResponses().keySet()) {
					ApiResponse response = source.getComponents().getResponses().get(key);
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
					if (response.getContent() != null) {
						for (String type : response.getContent().keySet()) {
							Schema<?> schema = response.getContent().get(type).getSchema();
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
										if (propertySchema.get$ref() != null) {
											String newSchema = schemaReplacements.get(propertySchema.get$ref());
											if (newSchema != null) {
												propertySchema.set$ref(newSchema);
											}
										}
									}
								}
							}
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
}
