# ADR 007: Bounded catalog delivery

Status: accepted

## Context

The storefront needs the active product catalog and offers before it can build a cart. Catalog replacement and checkout requests are already limited to 1,000 entries and 1 MiB of JSON. The current UI browses one small catalog without search or category filters.

Pagination affects more than the controller signature. It introduces ordering and cursor semantics, partial frontend state, search/filter behavior and reconciliation rules when the catalog revision changes between pages. Adding it without those decisions would move complexity into every client while providing no measured benefit for this bounded exercise.

## Decision

Return the complete active catalog in product-ID order, with a maximum of 1,000 products and the revision of the snapshot used for the response. Keep explicit refresh and `Cache-Control: no-store` for the public catalog. The management ETag remains a write-concurrency validator; the storefront does not retain a public cache validator.

Defer pagination until catalog size, search requirements or measured response latency justify it. Introduce it as one coordinated change covering the OpenAPI query and page envelope, deterministic keyset ordering, database indexes, filter semantics, Angular incremental state and revision reconciliation. Evaluate conditional GET with a public ETag in that same design so browser caching and refresh behavior remain explicit.

## Alternatives and consequences

Offset pagination is easy to expose but can duplicate or skip products while concurrent replacements change ordering. Keyset pagination is more stable, but it still needs a snapshot/revision policy across pages. Returning the bounded catalog keeps one consistent database read, one client state transition and simple checkout reconciliation.

Response size and transfer time grow linearly up to the enforced limit, and each refresh downloads the complete catalog. The 1,000-product bound is an explicit scope and resource limit, not a claim that this response shape fits every future supermarket catalog. Exceeding it requires revisiting this decision rather than silently increasing the cap.
