# ADR 004: active catalog from validated configuration

Status: retained for the explicit `config-catalog` fallback; default persistence superseded by [ADR 005](005-database-catalog.md).

## Context

The checkout needs a catalog and active quantity offers. The current scope has a small catalog, no administration UI and no persisted carts or orders. Offers can change between application runs.

## Decision

Bind `checkout.catalog` from Spring Boot configuration. Keep products and offers in separate lists so duplicate IDs, duplicate offers and missing product references are detected rather than overwritten by map binding. The bundled `application.yml` contains the demonstration catalog.

`CatalogProperties` validates required fields during binding. `CatalogConfiguration` creates domain values and a `CatalogSnapshot`, reusing domain price/quantity/reference validation. Invalid configuration fails application startup. Offers may be omitted when no offers are active; product configuration is required.

The configured provider holds one immutable snapshot. `findAll` exposes the complete bounded catalog; `findFor` returns only known requested products and their offers, with missing IDs absent and empty requests producing empty snapshots. The checkout application will reject missing requested IDs. The provider does not retain request collections or mutate its catalog.

Use external configuration to change the active catalog without rebuilding, then restart the application. Higher-priority Spring configuration replaces a list as a whole; supply the complete product/offer lists. To remove all bundled offers, explicitly set `offers: []` in the external YAML. There is no live reload or automatic calendar activation.

## Consequences

This source keeps local startup and testing simple, with explicit validation and no database service. It is suitable for this bounded demonstration, not a live product-management workflow.

A database becomes appropriate when independent editing, audit history, per-store pricing, search or coordinated updates are required. The application-owned provider port separates that source from pricing, but a database implementation also needs migrations, connection management, batched queries, integration tests and an explicit product/offer consistency strategy. An immutable return value alone cannot make separate database reads consistent. A larger catalog may also require coordinated API/UI pagination changes.
