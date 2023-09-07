A prototype Open API spec aggregator. This is a work in progress. Provides some autoconfiguration for the OpenAPI spec aggregator, exposing it at `/v3/api-docs`.

Given 2 existing public APIs at https://wizard-world-api.herokuapp.com/ and https://date.nager.at/ (for instance), we can configure the aggregator as follow:

```java
@Bean
OpenApiAggregatorSpecs specs() {
	return new OpenApiAggregatorSpecs()
			.spec(new Spec("https://date.nager.at/swagger/v3/swagger.json").replace("/api/v3", "/dates"))
			.spec(new Spec("https://wizard-world-api.herokuapp.com/swagger/v1/swagger.json").prefix("/wizards"));
}
```

The aggregator will then expose a spec that combines the two APIs. The Wizards API will be prefixed with `/wizards` and the Dates API will be prefixed with `/dates` (with an additional removal of the `/api/v3` prefix from the backend). The sample application `GatewayApplication` does this and also exposes the APIs via a Spring Cloud Gateway, so the paths match the aggregated spec. You can run it with `./mvnw spring-boot:test-run` and browse the spec at http://localhost:8080/v3/api-docs.

If any of the specs is Swagger (OpenAPI 2.0) instead of OpenAPI 3.0 the aggregator will automatically convert it to OpenAPI 3.0. OpenAPI 3.1 is not supported yet.