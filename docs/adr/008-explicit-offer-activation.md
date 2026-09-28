# ADR 008: Explicit offer activation

Status: accepted

## Context

The exercise requires weekly offers but does not define validity timestamps, a business time zone, overlap priority, future publication, audit history or behavior at boundary instants. Inferring those rules would create product behavior that cannot be verified against the brief.

PostgreSQL catalog replacement already changes the active products and offers atomically without restarting the application. The YAML fallback remains a startup configuration intended for lightweight demonstrations.

## Decision

Treat a weekly offer as part of the currently active catalog. An operator activates a new set through the protected GET/PUT workflow using ETag and `If-Match`; the replacement commits products, offers and a new revision in one transaction. Running storefronts observe it through explicit refresh or revision-aware checkout reconciliation. No application restart is required for the default PostgreSQL profile.

Do not add automatic calendar activation in this scope. A future scheduled model must define `effectiveFrom` and `effectiveUntil`, store instants in UTC, identify the business time zone used to author weekly periods, resolve overlaps deterministically, preserve audit/rollback history and change the effective catalog revision when time changes the visible offer set. Boundary-instant and clock-controlled tests are required.

## Alternatives and consequences

Adding nullable dates to the current offer row would look small but would leave overlap, priority, time-zone and revision semantics undefined. A scheduled job would also need ownership and recovery rules in a replicated deployment; query-time selection would need equally explicit consistency and indexing decisions.

The selected workflow is operationally manual but deterministic, restart-free and already protected by optimistic concurrency. The YAML fallback still requires a restart because it deliberately holds one immutable startup snapshot. Teams that need future publication or automatic weekly turnover must revisit this decision with product rules, rather than treating a timer as an isolated implementation detail.
