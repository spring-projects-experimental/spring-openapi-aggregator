A prototype Open API spec aggregator. Provides some autoconfiguration for the OpenAPI spec aggregator, exposing it at `/v3/api-docs`.

Given 2 existing public APIs at https://wizard-world-api.herokuapp.com/ and https://date.nager.at/ (for instance), we can configure the aggregator as follows:

```java
@Bean
OpenApiAggregatorSpecs specs() {
	return new OpenApiAggregatorSpecs()
			.spec(new Spec("https://date.nager.at/swagger/v3/swagger.json").replace("/api/v3", "/dates"))
			.spec(new Spec("https://wizard-world-api.herokuapp.com/swagger/v1/swagger.json").prefix("/wizards"));
}
```

The aggregator will then expose a spec that combines the two APIs. The Wizards API will be prefixed with `/wizards` and the Dates API will be prefixed with `/dates` (with an additional removal of the `/api/v3` prefix from the backend). The sample application `GatewayApplication` does this and also exposes the APIs via a Spring Cloud Gateway, so the paths match the aggregated spec. You can run it with `./mvnw spring-boot:test-run` and browse the spec at http://localhost:8080/v3/api-docs.

Features:

* Spec conversion. If any of the specs is Swagger (OpenAPI 2.0) instead of OpenAPI 3.0 the aggregator will automatically convert it to OpenAPI 3.0. OpenAPI 3.1 is not supported yet, but it probably wouldn't be hard.
* Spec filtering. You can apply arbitrary filters to component specs, and to the aggregated result. For instance, you can remove the `info` section from the aggregated spec, or remove the `paths` section from the Wizards API spec.
* Convenience methods for common filters. For instance, you can add path prefixes (as in the example above), rename operations, or rename schema objects. Cross references are updated automatically.
* External configuration. Set `spring.openapi.base.*` to be an `OpenAPI` spec that will be merged with the aggregated spec. This is useful for adding info, or global security definitions, for instance. And set `spring.openapi.aggregator.path` to configure the HTTP endpoint path (default `/v3/api-docs`). See `application.yml` in the tests for an example.