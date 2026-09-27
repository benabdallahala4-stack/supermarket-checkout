# Project context

Purpose: calculate supermarket checkout receipts with automatically applied quantity offers.

Current implementation: Java 21 / Spring Boot bootstrap, OpenAPI contract, generated Spring interfaces/models and Angular client, contract-boundary tests, validated immutable catalog/cart domain, ArchUnit boundaries, JaCoCo coverage, formatting, JAR build and backend/contract CI jobs. Stateless domain receipt calculation is implemented. The configured catalog provider loads validated YAML and returns immutable catalog subsets. MapStruct adapters, catalog/checkout HTTP endpoints and strict JSON/problem responses are implemented. Angular 22.2 application shell, strict generated-client compilation, ESLint/Prettier, Vitest, development proxy and frontend CI are implemented. The catalog page renders server prices and offers with loading, empty, error and retry states. Page-scoped cart signals and product/cart controls support adding, incrementing, decrementing, removing and clearing quantities, with immutable snapshots and int32 bounds. Explicit checkout requests and itemized receipts are implemented, including exact server amounts, applied offers, empty receipts, pending guards, retry after errors, snapshot-based invalidation and stale-response protection.

Target architecture: catalog and checkout features; PostgreSQL catalog with explicit YAML fallback; pure Java pricing; OpenAPI-generated REST interfaces/models and Angular client; MapStruct between REST and domain.

Scope: EUR, integer quantities, one active single-product offer, repeated bundles and regular-price remainder. PostgreSQL persistence is implemented; payments, inventory, management authentication and scheduling remain outside the current implementation.

Invariants: BigDecimal money; duplicate items aggregate; input ordering does not affect the receipt; subtotal minus discount equals total; frontend displays backend prices.

Verification: `./scripts/verify.sh all` (backend, contract and frontend), or each mode separately. Frontend requires Node 24.21.0 / npm 11.19.0; see frontend/.nvmrc.
Regenerate Angular client: `./gradlew :backend:generateFrontendApi`. Never edit generated sources.
Run: `./gradlew :backend:bootRun`.

CI: pull requests and pushes to main run backend, contract and frontend jobs under Java 21 and pinned Node. Hosted execution awaits repository publication.

Next milestones: protected atomic catalog updates, then public revision metadata and browser reconciliation; accessibility and final reviewer documentation follow. Frontend verification enforces 80% line coverage over handwritten checkout-feature code.

Persistence: Flyway initializes demo data once; JDBC reads products/offers/revision in one statement. Default runtime needs PostgreSQL; config-catalog is the explicit YAML profile. Backend verification requires Docker for Testcontainers. See ADR 005 and README.
