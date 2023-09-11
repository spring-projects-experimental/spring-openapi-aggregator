package com.example.aggregator;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenApiFilterChain implements OpenApiFilter {

	private final OpenApiFilter first;
	private final OpenApiFilter next;

	public OpenApiFilterChain(OpenApiFilter first, OpenApiFilter next) {
		this.first = first;
		this.next = next;
	}

	@Override
	public OpenAPI filter(OpenAPI api) {
		OpenAPI intermediate = first.filter(api);
		OpenAPI result = next.filter(intermediate);
		return result;
	}

	@Override
	public Map<String, String> getPathMappings() {
		return merge(first.getPathMappings(), next.getPathMappings());
	}

	@Override
	public Map<String, String> getOperationMappings() {
		return merge(first.getOperationMappings(), next.getOperationMappings());
	}

	@Override
	public Map<String, String> getSchemaMappings() {
		return merge(first.getSchemaMappings(), next.getSchemaMappings());
	}

	private Map<String, String> merge(Map<String, String> firstMappings,
			Map<String, String> nextMappings) {
		Map<String, String> result = new HashMap<>();
		for (String key : firstMappings.keySet()) {
			String value = firstMappings.get(key);
			if (nextMappings.containsKey(value)) {
				result.put(key, nextMappings.get(value));
			} else {
				result.put(key, value);
			}
		}
		for (String key : nextMappings.keySet()) {
			String value = nextMappings.get(key);
			if (!firstMappings.containsValue(value)) {
				result.put(key, value);
			}
		}
		return result;
	}

}
