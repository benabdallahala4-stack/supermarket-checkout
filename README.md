# Supermarket Checkout

A supermarket checkout that applies active quantity offers automatically. The backend owns pricing; the planned Angular interface submits product quantities and displays an itemized receipt.

## Current milestone

The Java 21 / Spring Boot application foundation is available. Checkout endpoints, pricing rules and the Angular interface are subsequent milestones.

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

This checks Java formatting, runs the startup test, and builds the executable JAR. The equivalent Gradle command is:

```sh
./gradlew --no-daemon :backend:check :backend:bootJar
```

Run the packaged application with:

```sh
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar
```

Format Java sources with `./gradlew :backend:spotlessApply`. Root `./gradlew build` also aggregates backend verification and packaging.

## Continuous integration

The [CI workflow](.github/workflows/ci.yml) runs on pull requests and pushes to `main`. It installs Temurin Java 21 on Ubuntu, caches Gradle dependencies, and calls the same `./scripts/verify.sh backend` command used locally. Formatting violations, failed tests or packaging failures fail the job.

Actions are pinned to commit revisions. The workflow has read-only repository permissions, does not retain checkout credentials, cancels superseded runs and has a 15-minute timeout. No secrets or deployment configuration are required.

The workflow definition is verified locally. Its first hosted execution requires publishing the repository to GitHub; no hosted result is claimed here.

## Structure and decisions

- `backend/`: Spring Boot application, Java tests and build configuration
- `gradle/wrapper/`: pinned Gradle distribution and download checksum
- `scripts/verify.sh`: shared local verification entry point
- `docs/adr/`: architectural decisions

See [application boundaries](docs/adr/001-application-boundaries.md), [root agent instructions](AGENTS.md) and [backend conventions](backend/AGENTS.md).
