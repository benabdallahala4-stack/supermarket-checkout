# ADR-001: Application boundaries

Status: Accepted

## Context

The exercise requires deterministic checkout pricing and a reviewer-friendly application. The planned UI and REST contract should not couple monetary rules to a framework. The owner selected Java 21, Spring Boot, Gradle and Angular.

## Decision

Use one repository with a Gradle backend and a separately built Angular frontend. Organize backend code by catalog and checkout feature, with API and application adapters around plain Java domain types. The backend owns pricing. The catalog may be consumed by checkout; it does not depend on checkout.

The first milestone establishes only the runnable backend and its verification entry point. Add contract generation, catalog configuration and UI alongside their tested behavior in later milestones.

## Consequences

Pricing can be tested without a Spring context, and frontend work uses an explicit HTTP boundary. The repository needs two build toolchains when the frontend arrives. This does not justify separate services, a database, or deployment infrastructure for the current scope.
