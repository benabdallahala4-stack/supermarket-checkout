# ADR-001: Application boundaries

Status: Accepted

## Context

The exercise requires deterministic checkout pricing and a reviewer-friendly application. The planned UI and REST contract should not couple monetary rules to a framework. The owner selected Java 21, Spring Boot, Gradle and Angular.

## Decision

Use one repository with a Gradle backend and a separately built Angular frontend. Organize backend code by catalog and checkout feature, with API and application adapters around plain Java domain types. The backend owns pricing. The catalog may be consumed by checkout; it does not depend on checkout.

The REST contract, catalog adapters and UI remain outside the pricing domain and are added alongside their tested behavior.

## Consequences

Pricing can be tested without a Spring context, and the frontend uses an explicit HTTP boundary. The repository needs Java and Node build toolchains. The application remains one deployable backend because separate services add no useful boundary for this scope. PostgreSQL is used for runtime catalog changes and persistence, while carts and receipts remain stateless.
