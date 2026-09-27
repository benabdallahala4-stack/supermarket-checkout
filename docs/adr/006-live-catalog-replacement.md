# ADR 006: Protected atomic catalog replacement

Status: accepted

## Context

The persisted catalog needs an explicit update workflow that changes active prices without restarting. A small operator API is sufficient; the storefront must not contain credentials. Concurrent operators must not silently overwrite each other.

## Decision

Add opt-in GET/PUT `/api/management/catalog` to the canonical OpenAPI contract and generate both Spring and Angular types. Use structural MapStruct conversion into the existing validated domain snapshot. The application service delegates to a CatalogEditor port, implemented with JDBC and an explicit transaction.

Return the catalog revision as a strong ETag and require it in `If-Match` for every replacement. The PUT body contains only the replacement items, keeping the HTTP precondition as the single concurrency input. A conditional UUID update locks the singleton revision row; missing preconditions receive 428 and stale writers receive 412. Delete/insert products and offers within that same transaction. Rollback restores all rows and metadata. Return a new revision and ETag even when content is unchanged.

Enable the endpoints only under catalog-management, with a non-whitespace token of at least 32 characters provided externally. Use a random token, not a human password. Compare token bytes with MessageDigest.isEqual. An MVC interceptor identifies the actual management controller handler and checks the header before argument binding; URL prefixes are not the security boundary. The profile binds to loopback by default and cannot use YAML storage. Missing credentials fail startup. No session/cookie authentication is used.

## Alternatives and consequences

A last-write-wins API is simpler but loses concurrent edits. Per-product patch endpoints require more conflict and offer-consistency rules than the whole-catalog workflow needs. Full user accounts and roles would introduce an identity subsystem; the selected shared token grants one operator capability and must remain outside the browser. Remote use requires deliberate network configuration and HTTPS.

An administration SPA and automatic refresh are independent concerns. The generated management client remains available for tooling, but the storefront does not call it. Public catalog and checkout responses carry read revisions so the storefront can discard a quote from a different catalog snapshot and reconcile its cart. No calendar scheduling is added here.

The operator guide documents recovery from stale revisions, lost responses and intentionally empty catalogs. Database integration tests demonstrate one winner for competing revisions, rollback, persistence and changed checkout prices without restart.
