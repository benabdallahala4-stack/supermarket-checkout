-- Initial demo data runs once per database, never when an existing catalog becomes empty.
INSERT INTO products (id, name, unit_price) VALUES
    ('APPLE', 'Apple', 0.30),
    ('BANANA', 'Banana', 0.50),
    ('ORANGE', 'Orange', 0.80);

INSERT INTO offers (product_id, bundle_quantity, bundle_price) VALUES
    ('APPLE', 2, 0.45),
    ('BANANA', 3, 1.20);
