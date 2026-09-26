# Supermarket Checkout

A supermarket checkout that applies active quantity offers automatically. The backend owns pricing; the planned Angular interface submits product quantities and displays an itemized receipt.

## Current milestone

The Java 21 / Spring Boot foundation and OpenAPI contract generation are available. Spring API interfaces/models and an Angular client are generated from one specification. Validated immutable catalog and cart values are available, with architecture tests enforcing domain boundaries. The pure Java calculator applies repeated quantity offers, aggregates duplicate items and returns immutable itemized receipts. The active catalog is loaded and validated from YAML at startup. HTTP endpoints and the Angular application are subsequent milestones.

## Prerequisites

- JDK 21, with `JAVA_HOME` pointing to it
- Bash on Linux, macOS or WSL for the verification script
- Internet access on the first build to download Gradle and dependencies

Gradle is supplied by the committed Wrapper; no global Gradle installation is needed. On Windows without WSL, use `gradlew.bat` for the equivalent Gradle commands.

## Run

From the repository root:

```sh
./gradlew :backend:bootRun
```

The application starts on port 8080. No product or checkout routes exist in this milestone, so requests to `/` return 404.

## Verify and package

```sh
./scripts/verify.sh backend
```

This validates the contract, generates and compiles Spring types, checks Java formatting, runs startup, contract-boundary, domain and architecture tests, enforces 80% aggregate line coverage over handwritten catalog/checkout classes, and builds the executable JAR. The equivalent Gradle command is:

```sh
./gradlew --no-daemon :backend:check :backend:bootJar
```

Run the packaged application with:

```sh
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar
```

Format Java sources with `./gradlew :backend:spotlessApply`. Root `./gradlew build` also aggregates backend verification and packaging.

Check the generated Angular client separately:

```sh
./scripts/verify.sh contract
```

After editing `api/openapi.yaml`, run `./gradlew :backend:generateFrontendApi` and review the generated diff. The check compares every generated client file without modifying it. See [API contract and generation](docs/api.md). Angular compilation will be verified when its application is scaffolded.

## Continuous integration

The [CI workflow](.github/workflows/ci.yml) runs on pull requests and pushes to `main`. Separate backend and contract jobs install Temurin Java 21 on Ubuntu and cache Gradle dependencies. They call `./scripts/verify.sh backend` and `./scripts/verify.sh contract`, the same commands used locally. Formatting violations, failed tests, coverage below the floor, architecture violations, packaging failures or generated-client drift fail their respective jobs.

Actions are pinned to commit revisions. The workflow has read-only repository permissions, does not retain checkout credentials, cancels superseded runs and has a 15-minute timeout. No secrets or deployment configuration are required.

The workflow definition is verified locally. Its first hosted execution requires publishing the repository to GitHub; no hosted result is claimed here.

## Structure and decisions

- `backend/`: Spring Boot application, Java tests and build configuration
- `api/openapi.yaml`: source contract for catalog and checkout
- `frontend/src/app/generated/api/`: generated Angular services/models (no application yet)
- `gradle/wrapper/`: pinned Gradle distribution and download checksum
- `scripts/verify.sh`: shared local verification entry point
- `docs/adr/`: architectural decisions

See [application boundaries](docs/adr/001-application-boundaries.md), [root agent instructions](AGENTS.md) and [backend conventions](backend/AGENTS.md).

## Domain verification

`backend:check` requires JaCoCo coverage verification and emits HTML and XML reports under `backend/build/reports/jacoco/test/`. The 80% aggregate line floor covers all classes in the catalog and checkout features, including future adapters; generated types and the bootstrap class are outside that business scope.

ArchUnit restricts domain dependencies to JDK/domain types and prevents catalog code from depending on checkout. Catalog tests cover exact cents, valid discounts, duplicate and unknown references, and immutable snapshots. Cart tests require a product ID and a positive integer quantity. See [monetary representation](docs/adr/003-monetary-representation.md).

## Pricing behavior

The calculator combines quantities for each product, rejects int32 overflow and unknown products, and returns lines sorted by product ID. Each offer applies to every complete bundle; remaining units use the regular price. An empty cart returns an empty receipt with exact zero totals. No unit-by-unit expansion is needed, including for the largest supported quantities.

For an apple priced at EUR 0.30 with two for EUR 0.45, three apples cost EUR 0.75 and five cost EUR 1.20. With bananas at EUR 0.50 and three for EUR 1.20, three apples plus four bananas cost EUR 2.45. These are executable domain examples and match the bundled active catalog.

JUnit tests cover named examples, input errors, duplicate entries, immutable receipts, large quantities and concurrent calls. Three jqwik properties each generate 300 bounded catalog/cart cases for receipt arithmetic, order independence and split-entry equivalence. Gradle test reports retain jqwik's seed and minimized sample when a property fails; reproduce a failure by temporarily setting that property's `seed` to the reported value. Local `.jqwik-database` replay state is ignored.

## Catalog configuration

The bundled `backend/src/main/resources/application.yml` supplies three products and two active offers. Product and offer lists bind through Spring Boot; missing fields, duplicate IDs/offers, unknown references and invalid monetary or quantity values fail startup. Domain validation remains the source of semantic rules.

To supply a different catalog, create an external YAML file with the same `checkout.catalog` structure, then start the packaged application with:

```sh
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:./config/catalog.yml
```

Supply complete lists because Spring replaces lists across configuration sources. Use `offers: []` to clear bundled offers. Changing the file requires a restart; no rebuild is needed. There is no automatic weekly activation or live reload. See [active catalog decision](docs/adr/004-active-catalog-configuration.md) and [scope assumptions](docs/assumptions.md).

The catalog provider returns immutable views and looks up a set of IDs together. Missing IDs are omitted for checkout to reject, rather than producing partial successful receipts. HTTP endpoints are still pending.
