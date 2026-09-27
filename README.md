# Supermarket Checkout

A supermarket checkout that applies active quantity offers automatically. The backend owns pricing; the Angular interface displays the catalog and active offers and manages cart quantities. An explicit checkout action retrieves itemized receipts and savings from the backend.

## Current milestone

The Spring Boot backend serves the product catalog and calculates itemized checkout receipts. It reads a persistent PostgreSQL catalog (with an explicit YAML fallback profile), applies quantity offers in a pure Java domain, and maps the OpenAPI-generated REST models with MapStruct. Strict JSON input validation and consistent problem responses are covered by HTTP integration tests. The Angular 22 catalog page uses the generated API service to display product prices and quantity offers, with loading, empty, error and retry states. Strict compilation, linting, formatting, Vitest and a feature coverage floor run in CI. The cart supports adding products, changing quantities, removing products and clearing all items. Calculate checkout displays the server-calculated receipt, including applied offers and savings. Pending guards, error recovery and stale-response protection are covered by frontend tests.

## Prerequisites

- JDK 21, with `JAVA_HOME` pointing to it
- Node 24.21.0 and its bundled npm 11.19.0, pinned in frontend/.nvmrc and frontend/package.json
- Docker-compatible runtime for PostgreSQL and backend integration tests
- Bash on Linux, macOS or WSL for the verification script
- Internet access on the first build to download Gradle and dependencies

Gradle is supplied by the committed Wrapper; no global Gradle installation is needed. On Windows without WSL, use `gradlew.bat` for the equivalent Gradle commands.

## Run

From the repository root (see Persistent catalog below for connection settings):

```sh
export CHECKOUT_DB_PASSWORD='choose-a-local-password'
docker compose up -d --wait
./gradlew :backend:bootRun
```

The application starts on port 8080:

```sh
curl http://localhost:8080/api/products
curl -H 'Content-Type: application/json' -d '{"items":[{"productId":"APPLE","quantity":3}]}' http://localhost:8080/api/checkout
```

The checkout response contains subtotal `"0.90"`, discount `"0.15"` and total `"0.75"`. The backend root path `/` has no UI; Angular is served separately during development.

## Verify and package

```sh
./scripts/verify.sh backend
```

This validates the contract, generates and compiles Spring types, checks Java formatting, runs startup, contract, domain, configuration, application, mapper, HTTP and architecture tests, enforces 80% aggregate line coverage over handwritten catalog/checkout classes, and builds the executable JAR. The equivalent Gradle command is:

```sh
./gradlew --no-daemon :backend:check :backend:bootJar
```

With PostgreSQL running and the same database environment variables set, run the packaged application with:

```sh
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar
```

Format Java sources with `./gradlew :backend:spotlessApply`. Root `./gradlew build` also aggregates backend verification and packaging.

Check the generated Angular client separately:

```sh
./scripts/verify.sh contract
```

After editing `api/openapi.yaml`, run `./gradlew :backend:generateFrontendApi` and review the generated diff. The check compares every generated client file without modifying it. See [API contract and generation](docs/api.md). The generated client is compiled with the application and exercised through Angular HTTP tests.

## Continuous integration

The [CI workflow](.github/workflows/ci.yml) runs on pull requests and pushes to `main`. Separate backend, contract and frontend jobs use the same scripts as local verification. Java jobs use Temurin 21 and Gradle caching; the frontend job uses the pinned Node version and npm download caching. It runs npm ci, formatting, linting, tests with coverage, and the production build. Formatting violations, failed tests, coverage below an active threshold, architecture violations, compilation/bundle-budget failures or generated-client drift fail their respective jobs.

Actions are pinned to commit revisions. The workflow has read-only repository permissions, does not retain checkout credentials, cancels superseded runs and has a 15-minute timeout. No secrets or deployment configuration are required.

The workflow definition is verified locally. Its first hosted execution requires publishing the repository to GitHub; no hosted result is claimed here.

## Structure and decisions

- `backend/`: Spring Boot application, Java tests and build configuration
- `api/openapi.yaml`: source contract for catalog and checkout
- `frontend/`: Angular checkout page, product-list/cart/receipt components, quantity state, strict configuration, tests, lint/format rules and generated services/models
- `gradle/wrapper/`: pinned Gradle distribution and download checksum
- `scripts/verify.sh`: shared local verification entry point
- `docs/adr/`: architectural decisions

See [application boundaries](docs/adr/001-application-boundaries.md), [root agent instructions](AGENTS.md) and [backend conventions](backend/AGENTS.md).

## Domain verification

`backend:check` requires JaCoCo coverage verification and emits HTML and XML reports under `backend/build/reports/jacoco/test/`. The 80% aggregate line floor covers all classes in the catalog and checkout features, including future adapters; OpenAPI/MapStruct-generated types and the bootstrap class are outside that business scope.

ArchUnit restricts domain dependencies to JDK/domain types and prevents catalog code from depending on checkout. Catalog tests cover exact cents, valid discounts, duplicate and unknown references, and immutable snapshots. Cart tests require a product ID and a positive integer quantity. See [monetary representation](docs/adr/003-monetary-representation.md).

## Pricing behavior

The calculator combines quantities for each product, rejects int32 overflow and unknown products, and returns lines sorted by product ID. Each offer applies to every complete bundle; remaining units use the regular price. An empty cart returns an empty receipt with exact zero totals. No unit-by-unit expansion is needed, including for the largest supported quantities.

For an apple priced at EUR 0.30 with two for EUR 0.45, three apples cost EUR 0.75 and five cost EUR 1.20. With bananas at EUR 0.50 and three for EUR 1.20, three apples plus four bananas cost EUR 2.45. These are executable domain examples and match the bundled active catalog.

JUnit tests cover named examples, input errors, duplicate entries, immutable receipts, large quantities and concurrent calls. Three jqwik properties each generate 300 bounded catalog/cart cases for receipt arithmetic, order independence and split-entry equivalence. Gradle test reports retain jqwik's seed and minimized sample when a property fails; reproduce a failure by temporarily setting that property's `seed` to the reported value. Local `.jqwik-database` replay state is ignored.

## YAML fallback catalog

With the `config-catalog` profile, bundled `backend/src/main/resources/application-config-catalog.yml` supplies three products and two active offers. Product and offer lists bind through Spring Boot; missing fields, duplicate IDs/offers, unknown references and invalid monetary or quantity values fail startup. Domain validation remains the source of semantic rules.

To supply a different catalog, create an external YAML file with the same `checkout.catalog` structure, then start the packaged application with:

```sh
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=config-catalog --spring.config.additional-location=file:./config/catalog.yml
```

Supply complete lists because Spring replaces lists across configuration sources. Use `offers: []` to clear bundled offers. Changing the file requires a restart; no rebuild is needed. There is no automatic weekly activation or live reload. See [active catalog decision](docs/adr/004-active-catalog-configuration.md) and [scope assumptions](docs/assumptions.md).

The catalog provider returns immutable views and looks up a set of IDs together. Missing IDs are omitted for checkout to reject, rather than producing partial successful receipts. The checkout service performs one batch lookup per request and rejects unknown products before pricing.

## Frontend development

Use Node 24.21.0 with npm 11.19.0 for this project. If you use nvm, select the pinned runtime with `nvm install` and `nvm use` inside `frontend/`. Otherwise install that Node version with your preferred runtime manager. Do not rely on an older global Node installation; npm enforces the package engines.

Keep the backend running in one terminal. In another terminal:

```sh
cd frontend
npm ci
npm start
```

Open `http://localhost:4200`. The catalog shows the configured products, exact unit prices and active quantity offers. Failed requests show a retry action; an empty catalog has an explicit message. Use Add and the cart quantity buttons to build a cart; decreasing the last unit removes that product. Remove deletes a whole line and Clear cart empties it. Quantities are bounded to positive int32 values, and Add/increase controls are disabled at the limit. Cart state belongs to the page and resets on reload; it stores no prices. Select Calculate checkout to request a receipt, including for an empty cart. Cart edits and duplicate submissions are blocked while calculation is pending. An error preserves quantities and allows another calculation. Editing the cart afterward hides the previous receipt until you calculate again. The development server proxies `/api/**` to `http://127.0.0.1:8080`. Generated services use the relative `/api` base path, so no wildcard CORS configuration or duplicated `/api/api` prefix is needed. The production output is in `frontend/dist/supermarket-checkout/browser`; production hosting would need equivalent API routing.

With the pinned Node runtime active, run from the repository root:

```sh
./scripts/verify.sh frontend
./scripts/verify.sh all
```

Frontend verification performs a clean lockfile install, Prettier check, Angular/TypeScript/template lint, Vitest with coverage and a production build. The all mode requires backend, contract and frontend checks to succeed. Use `npm run format` from frontend/ to format handwritten files.

Generated client files are excluded from handwritten linting/formatting and coverage, but remain included in strict TypeScript compilation and generation-drift checks. Coverage reports are under `frontend/coverage/`. Tests enforce an 80% line coverage floor over handwritten checkout-feature code. They exercise catalog loading, offers, empty responses, errors, retry guards and request cancellation using the real generated service with Angular HTTP testing. Cart tests cover quantity controls, count/empty state, int32 bounds, immutable snapshots, page-scoped lifetime and sorted checkout request projection without money calculations. Receipt tests verify exact server strings, empty checkout, pending guards, error recovery, invalidation, delayed/out-of-order responses and destruction cleanup. Responses are accepted only for the current request ID and immutable cart snapshot; the frontend never recalculates monetary amounts. Production bundle budgets are already enforced.

The framework configuration follows the [Angular compatibility requirements](https://angular.dev/reference/versions) and the [CLI-supported Vitest coverage workflow](https://angular.dev/guide/testing/code-coverage).

## Persistent catalog

The default backend uses PostgreSQL. With Docker available, run from the repository root:

```sh
export CHECKOUT_DB_PASSWORD='choose-a-local-password'
docker compose up -d --wait
./gradlew :backend:bootRun
```

The password stays in your shell environment. The default connection is localhost:5432, database/user `checkout`; override `CHECKOUT_DB_URL` and `CHECKOUT_DB_USER` if needed. If changing `CHECKOUT_DB_PORT` for Compose, also adjust the JDBC URL. Stop with `docker compose down`; the named volume retains data. Flyway initializes demo products once and preserves subsequent edits and intentionally empty catalogs.

For the YAML demonstration without Docker:

```sh
./gradlew :backend:bootRun --args='--spring.profiles.active=config-catalog'
```

`./scripts/verify.sh database` runs PostgreSQL integration tests. The backend/all verification modes include them and require a running Docker-compatible runtime. Tests create isolated databases; they do not use your development catalog. Protected runtime management is available through the optional operator profile; the storefront supports explicit refresh and revision-aware receipt reconciliation. See [operator workflow](docs/catalog-management.md).

## Live catalog updates

Use the opt-in [operator workflow](docs/catalog-management.md) to replace products and offers without restarting. It requires a secret token and the revision you read, rejects stale edits with 409, and commits rows and revision atomically. The storefront does not hold this credential.

## Refreshing prices

Select **Refresh products** to reload the catalog. This clears the displayed receipt, retains quantities for available products, and reports removed products. If a checkout detects changed prices, it reloads the catalog and asks you to calculate again. Failed refreshes preserve quantities and offer retry. Quotes are not price reservations; no background polling is performed. See [revision semantics](docs/api.md#catalog-revision-and-quote-reconciliation).

## Accessibility

Keyboard focus is maintained across cart removal, refresh and checkout. Narrow layouts wrap long product names and exact amounts. See the [accessibility review](docs/accessibility.md) for the tested behavior and evidence.
