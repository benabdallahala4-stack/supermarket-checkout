# Checkout API contract

The source of truth is [api/openapi.yaml](../api/openapi.yaml). It defines the target API; controllers are not implemented in the current milestone.

## Operations

| Operation | Request | Successful response |
| --- | --- | --- |
| `GET /api/products` (`getProducts`) | None | `currency`, sorted `items`, `totalItems` |
| `POST /api/checkout` (`calculateCheckout`) | `items` containing `productId` and positive integer `quantity` | Itemized receipt and subtotal, discount, total |

All monetary values are nonnegative EUR strings with exactly two decimal places. Product identifiers are case-sensitive and nonblank. Optional `offer` and `appliedOffer` properties are omitted when absent. Requests reject unknown fields. Response objects permit additive properties for forward compatibility.

An empty items array is valid and produces a receipt with no lines and `"0.00"` amounts. Missing/null arrays or entries are invalid. Duplicate product quantities aggregate before pricing, and their sum must fit int32. Unknown products reject the whole request. Decimal or quoted quantity tokens must be rejected by the eventual HTTP adapter; OpenAPI's integer schema and generated models alone do not enforce this lexical rule.

The pinned generator emits nested `@Valid` items without element-level `@NotNull`. The HTTP adapter milestone must explicitly reject `items: [null]` and cover it with an integration test, alongside unknown-field and numeric-coercion rejection. Do not manually patch generated classes to implement these policies.

Checkout is stateless. It applies each complete bundle, then charges remaining units at regular price. For three apples priced at `"0.30"` with two for `"0.45"`, subtotal is `"0.90"`, discount is `"0.15"`, and total is `"0.75"`. Request/response examples, including empty checkout and errors, are embedded in the specification.

Errors use `application/problem+json`, including `type`, `title`, `status`, `detail`, `instance`, `code`, and optional field errors. The contract defines 400 for invalid input/unknown products, 415 for unsupported checkout content types, and 500 for safe generic server errors. The future error handler must implement these responses explicitly.

## Reproducible generation

OpenAPI Generator 7.15.0 is pinned in the backend Gradle build and generates both sides. Spring interfaces/models are build output, never committed. Handwritten controllers will implement those interfaces. The Angular TypeScript client is committed so frontend users will not need Java for ordinary npm builds.

```sh
./gradlew :backend:openApiValidate
./gradlew :backend:openApiGenerate
./gradlew :backend:generateFrontendApi
./scripts/verify.sh contract
```

`generateFrontendApi` synchronizes generated TypeScript files into `frontend/src/app/generated/api`; it removes obsolete files in that generated directory. Never put handwritten code there. Generator metadata, package scaffolding, documentation and timestamps are excluded from committed output.

`checkFrontendApi` generates into a separate build directory, then compares full file lists and exact bytes. Missing, extra (including untracked) and changed files fail the check. It does not update the committed client. Backend compilation regenerates Spring types from the same specification.

The base URL is `/api`, and operation paths are `/products` and `/checkout`. The generated Spring interfaces carry the base mapping; future controllers must not add a second `/api`. Configure the Angular client's relative base path through its generated provider when the application is scaffolded.

## Current verification boundary

This milestone validates the specification, compiles generated Java, checks generated-model behavior and client reproducibility. Angular client compilation is deferred until the Angular 22 application is scaffolded. No HTTP endpoint behavior or pricing implementation is claimed yet.
