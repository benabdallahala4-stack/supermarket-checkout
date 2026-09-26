# ADR-002: Contract-first OpenAPI generation

Status: Accepted

## Context

Backend and frontend share request/response shapes and monetary conventions. The owner selected generated APIs/models on both sides, with handwritten domain code kept separate.

## Decision

Maintain one OpenAPI specification under `api/`. Pin one generator version for Spring interfaces/models and Angular client/models. Compile backend generated sources from `backend/build/`. Commit only generated Angular TypeScript sources to allow independent frontend npm builds. Implement generated Spring interfaces directly; MapStruct mappings arrive with the HTTP adapter milestone.

Money is represented as a constrained decimal string on the wire, not a JSON number. Missing optional offers are omitted. Disable generated timestamps and check complete frontend file sets and contents against fresh build-directory output in CI.

## Consequences

Contract changes become explicit and compile-time model changes reach both consumers. Generated source must not be edited manually; regeneration and review are required. Committed client files add diff volume but avoid a Java prerequisite for normal frontend builds. Generator upgrades require compatibility review and regeneration. Generated validation cannot replace HTTP coercion tests or domain rules.
