# Project context

Purpose: calculate supermarket checkout receipts with automatically applied quantity offers.

Current implementation: Java 21 / Spring Boot bootstrap, OpenAPI contract, generated Spring interfaces/models and Angular client, contract-boundary tests, validated immutable catalog/cart domain, ArchUnit boundaries, JaCoCo coverage, formatting, JAR build and backend/contract CI jobs. Stateless domain receipt calculation is implemented. The configured catalog provider loads validated YAML and returns immutable catalog subsets. MapStruct adapters, catalog/checkout HTTP endpoints and strict JSON/problem responses are implemented. No Angular application yet.

Target architecture: catalog and checkout features; immutable configured catalog; pure Java pricing; OpenAPI-generated REST interfaces/models and Angular client; MapStruct between REST and domain.

Scope: EUR, integer quantities, one active single-product offer, repeated bundles and regular-price remainder. No persistence, payments, inventory, authentication or scheduling engine.

Invariants: BigDecimal money; duplicate items aggregate; input ordering does not affect the receipt; subtotal minus discount equals total; frontend displays backend prices.

Verification: `./scripts/verify.sh backend` and `./scripts/verify.sh contract`.
Regenerate Angular client: `./gradlew :backend:generateFrontendApi`. Never edit generated sources.
Run: `./gradlew :backend:bootRun`.

CI: pull requests and pushes to main run backend and contract verification under Java 21. Hosted execution awaits repository publication. Angular compilation is deferred until its application exists.

Next milestone: Angular application scaffold, compatible Node runtime, frontend checks and CI.
