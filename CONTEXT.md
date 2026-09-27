# Project context

Purpose: calculate supermarket checkout receipts with automatically applied quantity offers.

Current implementation: Java 21 / Spring Boot backend, PostgreSQL 16 / Flyway catalog, OpenAPI-generated Spring interfaces/models and Angular client, MapStruct REST adapters, and Angular 22.2 storefront. Pricing is a framework-independent Java domain. The default runtime reads one consistent database catalog snapshot; the `config-catalog` profile provides a validated YAML fallback. The opt-in `catalog-management` profile supports protected, revision-checked atomic catalog replacement without restart.

The storefront renders server prices and offers, manages page-scoped cart quantities, and displays backend-calculated itemized receipts. Loading, empty, pending, retry and catalog-change states are explicit. Catalog revisions prevent a receipt calculated from a different snapshot from being displayed as current.

Target architecture: catalog and checkout features; PostgreSQL catalog with explicit YAML fallback; pure Java pricing; OpenAPI-generated REST interfaces/models and Angular client; MapStruct between REST and domain.

Scope: EUR, integer quantities, one active single-product offer, repeated bundles and regular-price remainder. Payments, orders, inventory, automatic scheduling and user accounts are outside the implementation.

Invariants: BigDecimal money; duplicate items aggregate; input ordering does not affect the receipt; subtotal minus discount equals total; frontend displays backend prices.

Verification: `./scripts/verify.sh all` (backend, contract and frontend), or each mode separately. Frontend requires Node 24.21.0 / npm 11.19.0; see frontend/.nvmrc.
Regenerate Angular client: `./gradlew :backend:generateFrontendApi`. Never edit generated sources.
Run: `./gradlew :backend:bootRun`.

CI: pull requests and pushes to main run backend, contract and frontend jobs under Java 21 and pinned Node. Hosted execution awaits repository publication.

Coverage: backend verification enforces 80% aggregate line coverage over handwritten catalog/checkout classes. Frontend verification enforces 80% line coverage over handwritten checkout-feature code.

Persistence: Flyway initializes demo data once; JDBC reads products/offers/revision in one statement. Default runtime needs PostgreSQL; config-catalog is the explicit YAML profile. Backend verification requires Docker for Testcontainers. See ADR 005 and README.

Management: opt-in `catalog-management` profile, loopback by default, externally supplied token, generated GET/PUT contract, ETag/If-Match optimistic concurrency with `412` for stale writes and `428` for a missing precondition, atomic replacement and rollback. See docs/catalog-management.md.

Public responses carry catalogRevision from the same read snapshot. Storefront refresh clears quotes, retains available quantities, reports removals; revision mismatch/unknown products trigger refresh and explicit recalculation. No polling. Failed refresh preserves quantities and blocks checkout until retry.

Accessibility: stable focus targets for refresh/retry, removal and checkout; long-content reflow at 320px and 200% text enlargement. The scoped audit is documented in docs/accessibility.md.
