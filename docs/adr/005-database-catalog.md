# ADR 005: Persistent catalog reads

Status: accepted

## Context

The configured catalog validates useful demo data, but changing products or offers requires a restart. Persistent storage is the foundation for a later controlled update workflow. Pricing remains independent of storage and Spring.

## Decision

Use PostgreSQL 16, Flyway migrations and a JDBC implementation of CatalogProvider by default. Read requested products, their offers and a UUID catalog revision in one SQL statement. The revision row remains available for empty selections and empty catalogs. A later write workflow must change data and revision in the same transaction.

Store monetary values as unconstrained NUMERIC with checks for finite, nonnegative exact cents. This avoids both fixed-scale rounding and the narrower range introduced by integer cents. Domain validation also rejects offers that do not save money.

V2 initializes the example catalog once through Flyway history. Never reseed based on table emptiness: deleting all products must survive restart. Existing installations receive future migrations; editing applied migrations is not an update mechanism.

Keep the explicit config-catalog profile for the YAML demonstration and isolated HTTP tests. It disables datasource and Flyway auto-configuration. There is no H2 alternative: persistence tests run against PostgreSQL using Testcontainers and require Docker.

## Alternatives and consequences

JPA adds entity lifecycle machinery that this read model does not need. JDBC keeps the batched query and consistency boundary visible. Multiple queries under ordinary READ COMMITTED could observe different committed catalog states, so a read-only transaction alone is insufficient.

PostgreSQL adds an operational dependency. Compose provides a local service with a persistent volume and a password supplied through the environment. Verification includes database tests and fails if Docker is unavailable.

This milestone does not expose a management API or revision through REST. Those follow with atomic writes, access protection and browser reconciliation. Scheduling remains deferred; a stored write revision alone would not identify time-driven changes in effective offers.

References: [PostgreSQL transaction isolation](https://www.postgresql.org/docs/16/transaction-iso.html), [exact numeric types](https://www.postgresql.org/docs/16/datatype-numeric.html).
