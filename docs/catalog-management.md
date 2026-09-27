# Updating the catalog without restarting

Management is disabled by default. It requires PostgreSQL and the explicit `catalog-management` profile. It cannot be combined with `config-catalog`.

## Start operator mode

With the database running as described in README:

```sh
export CHECKOUT_CATALOG_TOKEN="$(openssl rand -hex 32)"
./gradlew :backend:bootRun --args='--spring.profiles.active=catalog-management'
```

Keep the token in the operator environment; never commit it or configure it in the Angular storefront. Operator mode binds to 127.0.0.1 by default, on the normal application port. It is not an Actuator endpoint or a separate management server. Remote deployment must deliberately configure network access and HTTPS; the shared token grants full catalog replacement authority. There are no individual accounts, roles or administration UI. Changing the token requires restarting operator mode, but product/offer changes do not.

## Read, edit, replace

In another shell with the same token available, this helper supplies the header through curl's standard input rather than its process arguments:

```sh
catalog_request() {
  printf 'header = "X-Catalog-Token: %s"\n' "$CHECKOUT_CATALOG_TOKEN" |
    curl --fail-with-body --config - "$@"
}

catalog_request http://127.0.0.1:8080/api/management/catalog > /tmp/catalog.json
```

The response has `revision` and `items`. Each item has `id`, `name`, `unitPrice` and an optional `offer` with `quantity` and `price`. Prices are EUR strings with exactly two decimal places. Edit `/tmp/catalog.json`, preserving the revision you read. Then submit the complete replacement:

```sh
catalog_request -X PUT -H 'Content-Type: application/json' \
  --data-binary @/tmp/catalog.json \
  http://127.0.0.1:8080/api/management/catalog
```

The response contains the committed catalog and its new revision. Public product reads and subsequent checkouts use the new prices immediately. The storefront discovers changes when the user selects Refresh products or calculates checkout. A receipt from a different revision is discarded, products are refreshed, and the user is asked to calculate again. There is no background polling.

An empty `items` array intentionally removes every product and offer. Omitted products are deleted; omitted offers are removed. Empty catalogs and edits survive application restarts. This operation does not store carts, receipts or orders.

## Failure and concurrency policy

- 400: invalid JSON, revision, products or offers; nothing changes.
- 401: absent or incorrect token; the request body is not processed.
- 409: another replacement committed since the supplied revision. Read again and resolve your edits; do not blindly retry with a fresh revision.
- 415: send application/json.
- 500: unexpected failure. Database failures roll back the transaction. A failure after commit or a lost response can leave the outcome uncertain; read the catalog before retrying.

The database serializes replacements through one conditional revision update. Products, offers and revision commit together; insertion errors roll them all back. Two writers using the same revision have exactly one winner. Even identical content receives a new revision. Direct SQL updates bypass these application guarantees and are not the supported editing workflow.

The contract and generated backend/frontend types live under the existing OpenAPI pipeline. Generating a management client does not authorize the storefront; the UI never supplies an operator credential. Management responses use Cache-Control: no-store.

## Verification

`./scripts/verify.sh all` includes PostgreSQL concurrency/rollback/restart tests, invalid-input and authorization tests, real HTTP access checks, default-disabled behavior, contract generation consistency and frontend checks. Docker is required. Scheduling, date windows and mixed-product offers remain outside this workflow.
