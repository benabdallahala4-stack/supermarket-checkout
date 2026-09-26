# Scope and assumptions

- The catalog uses EUR, exact cents and integer product quantities. IDs are case-sensitive and are not normalized.
- There is at most one active single-product quantity offer. Every complete bundle receives the offer price; remaining units use the regular price. No cross-product, percentage or competing offers are supported.
- Free products and free bundles are permitted, but an offer must still save money against its product's regular bundle price.
- The active catalog is selected at startup. Weekly changes mean updating configuration and restarting; scheduling and live administration are outside the current scope.
- Products and offers are separate lists. Product configuration is required; offers can be omitted when no other configuration source supplies them. Use an explicit empty list to clear inherited offers.
- External configuration replaces whole lists, not individual product records. A changed catalog does not affect a running process until restart.
- A provider lookup returns the known requested IDs and their offers as an immutable view. Missing IDs are not substituted or silently priced; checkout rejects them. An empty lookup returns an empty view.
- There is no persistence of carts, receipts, orders or payments, and no stock reservation, authentication or multi-currency support.
- A durable or independently managed catalog would require database consistency, migrations and integration tests. Search/pagination would also require coordinated API and UI work.
