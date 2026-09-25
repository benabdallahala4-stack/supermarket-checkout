# Project context

Purpose: calculate supermarket checkout receipts with automatically applied quantity offers.

Current implementation: Java 21 / Spring Boot bootstrap, Gradle Wrapper, formatting check, startup test, executable JAR build and backend CI workflow. No pricing, HTTP business endpoints or frontend yet.

Target architecture: catalog and checkout features; immutable configured catalog; pure Java pricing; OpenAPI-generated REST interfaces/models and Angular client; MapStruct between REST and domain.

Scope: EUR, integer quantities, one active single-product offer, repeated bundles and regular-price remainder. No persistence, payments, inventory, authentication or scheduling engine.

Invariants: BigDecimal money; duplicate items aggregate; input ordering does not affect the receipt; subtotal minus discount equals total; frontend displays backend prices.

Verification: `./scripts/verify.sh backend`.
Run: `./gradlew :backend:bootRun`.

CI: pull requests and pushes to main run the backend verification command under Java 21. Hosted execution awaits repository publication.

Next milestone: OpenAPI contract and generation.
