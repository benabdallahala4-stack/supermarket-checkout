# Checkout API contract

The source of truth is [api/openapi.yaml](../api/openapi.yaml). Handwritten controllers implement its generated Spring interfaces.

## Operations

| Operation | Request | Successful response |
| --- | --- | --- |
| `GET /api/products` (`getProducts`) | None | `currency`, sorted `items`, `totalItems` |
| `POST /api/checkout` (`calculateCheckout`) | `items` containing `productId` and positive integer `quantity` | Itemized receipt and subtotal, discount, total |

All monetary values are nonnegative EUR strings with exactly two decimal places. Product identifiers are case-sensitive and nonblank. Optional `offer` and `appliedOffer` properties are omitted when absent. Requests reject unknown fields. Response objects permit additive properties for forward compatibility.

An empty items array is valid and produces a receipt with no lines and `"0.00"` amounts. Missing/null arrays or entries are invalid. Duplicate product quantities aggregate before pricing, and their sum must fit int32. Unknown products reject the whole request. The HTTP adapter rejects decimal and quoted quantity tokens. Jackson coercion is explicitly disabled; the generated integer model alone would not enforce this lexical rule.

The pinned generator emits nested `@Valid` items without element-level `@NotNull`. Mapping creates a CheckoutCommand whose constructor rejects null entries, so `items: [null]` returns 400. MockMvc tests also verify missing fields, whitespace IDs, invalid numeric tokens, unknown fields and trailing JSON. Generated classes are unchanged.

Checkout is stateless. It applies each complete bundle, then charges remaining units at regular price. For three apples priced at `"0.30"` with two for `"0.45"`, subtotal is `"0.90"`, discount is `"0.15"`, and total is `"0.75"`. Request/response examples, including empty checkout and errors, are embedded in the specification.

Errors use `application/problem+json`, including `type`, `title`, `status`, `detail`, `instance`, `code`, and optional field errors. The contract defines 400 for invalid input/unknown products, 415 for unsupported checkout content types, and 500 for safe generic server errors. The exception handler returns these responses explicitly, with field errors sorted by field and message. Unexpected failures are logged internally and return generic details; internal IllegalArgumentException failures are not misclassified as bad client input.

## Reproducible generation

OpenAPI Generator 7.15.0 is pinned in the backend Gradle build and generates both sides. Spring interfaces/models are build output, never committed. Handwritten controllers implement those interfaces. The Angular TypeScript client is committed so frontend users will not need Java for ordinary npm builds.

```sh
./gradlew :backend:openApiValidate
./gradlew :backend:openApiGenerate
./gradlew :backend:generateFrontendApi
./scripts/verify.sh contract
```

`generateFrontendApi` synchronizes generated TypeScript files into `frontend/src/app/generated/api`; it removes obsolete files in that generated directory. Never put handwritten code there. Generator metadata, package scaffolding, documentation and timestamps are excluded from committed output.

`checkFrontendApi` generates into a separate build directory, then compares full file lists and exact bytes. Missing, extra (including untracked) and changed files fail the check. It does not update the committed client. Backend compilation regenerates Spring types from the same specification.

The base URL is `/api`, and operation paths are `/products` and `/checkout`. The generated Spring interfaces carry the base mapping; controllers do not add a second `/api`. Configure the Angular client's relative base path through its generated provider when the application is scaffolded.

## Mapping and application flow

MapStruct 1.6.3 generates the REST mappers with Spring constructor injection and unmapped target errors enabled. Request models map to CheckoutCommand/CartItem without aggregating quantities. The checkout service makes one batch catalog lookup, rejects missing products, and delegates to PricingCalculator. Response mappings preserve calculated values, rename bundlePrice to price and serialize exact two-decimal money strings. Optional applied offers map to absent JSON properties.

Catalog response assembly sorts products by ID and associates their active offers before returning the envelope. Mappers contain structural conversions only; no offer calculation, catalog retrieval or HTTP status selection. No persistence entity or unused reverse response mapping is introduced.

MapStruct implementations are generated under the backend build directory and are never manually edited. The coverage gate measures handwritten catalog/checkout classes and excludes these generated implementations.

## Current verification boundary

Backend verification covers configuration, pricing, batch coordination, real generated mapper beans, strict HTTP input, response serialization and safe failures. Tests use the Spring application with MockMvc; only the catalog provider is substituted for empty-catalog and internal-error scenarios. A packaged-JAR HTTP smoke test verifies the catalog and three-apple flow.

Contract validation and generated-client reproducibility also pass locally. Angular compilation awaits the Angular 22 application scaffold; hosted CI awaits publication.
