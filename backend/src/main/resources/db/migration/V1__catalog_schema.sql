CREATE TABLE catalog_revision (
    id integer PRIMARY KEY CHECK (id = 1),
    version uuid NOT NULL
);

INSERT INTO catalog_revision (id, version) VALUES (1, gen_random_uuid());

CREATE TABLE products (
    id text PRIMARY KEY CHECK (id ~ '[^[:space:]]'),
    name text NOT NULL CHECK (name ~ '[^[:space:]]'),
    unit_price numeric NOT NULL CHECK (
        unit_price >= 0
        AND unit_price NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND unit_price = trunc(unit_price, 2)
    )
);

CREATE TABLE offers (
    product_id text PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    bundle_quantity integer NOT NULL CHECK (bundle_quantity >= 2),
    bundle_price numeric NOT NULL CHECK (
        bundle_price >= 0
        AND bundle_price NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND bundle_price = trunc(bundle_price, 2)
    )
);
