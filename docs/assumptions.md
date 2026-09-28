# Scope and assumptions

- The catalog uses EUR, exact cents and integer product quantities. IDs are case-sensitive and are not normalized.
- API requests are bounded to 1,000 product/cart entries and 1 MiB of JSON. A database integration test proves a 1,000-product replacement with representative names and offers. The complete-catalog response and pagination threshold are recorded in [ADR 007](adr/007-bounded-catalog-delivery.md).
- There is at most one active single-product quantity offer. Every complete bundle receives the offer price; remaining units use the regular price. No cross-product, percentage or competing offers are supported.
- Free products and free bundles are permitted, but an offer must still save money against its product's regular bundle price.
- The default catalog is persisted in PostgreSQL and read per request. An optional protected replacement API updates products and offers without restart; scheduled activation is deliberately deferred in [ADR 008](adr/008-explicit-offer-activation.md). The optional YAML profile selects its catalog at startup.
- In the YAML profile, products and offers are separate lists. Product configuration is required; offers can be omitted when no other configuration source supplies them. Use an explicit empty list to clear inherited offers.
- In the YAML profile, external configuration replaces whole lists, not individual product records. A changed catalog does not affect a running process until restart.
- A provider lookup returns the known requested IDs and their offers as an immutable view. Missing IDs are not substituted or silently priced; checkout rejects them. An empty lookup returns an empty view.
- There is no persistence of carts, receipts, orders or payments, and no stock reservation, individual user accounts or multi-currency support. Catalog management uses one shared operator token.
- Persistent catalog reads include migrations, consistent batched reads and PostgreSQL integration tests. Search and pagination require the coordinated API, database and UI design described in [ADR 007](adr/007-bounded-catalog-delivery.md).
