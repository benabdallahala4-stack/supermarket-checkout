package com.example.supermarket.catalog.persistence;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.CatalogSnapshot;
import com.example.supermarket.catalog.domain.Product;
import com.example.supermarket.catalog.domain.ProductId;
import com.example.supermarket.catalog.domain.QuantityOffer;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!config-catalog")
public class JdbcCatalogProvider implements CatalogProvider {
  private final JdbcClient jdbc;

  public JdbcCatalogProvider(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public CatalogSnapshot findAll() {
    return read("true", Set.of());
  }

  @Override
  public CatalogSnapshot findFor(Set<ProductId> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("Requested product IDs are required");
    }

    for (ProductId id : ids) {
      if (id == null) {
        throw new IllegalArgumentException("Requested product ID must not be null");
      }
    }

    return read(ids.isEmpty() ? "false" : "p.id IN (:ids)", ids);
  }

  private CatalogSnapshot read(String selection, Set<ProductId> ids) {
    // One statement gives products, offers and revision the same PostgreSQL snapshot.
    var query =
        jdbc.sql(
            """
        SELECT r.version::text AS revision, p.id, p.name, p.unit_price,
               o.bundle_quantity, o.bundle_price
        FROM catalog_revision r
        LEFT JOIN products p ON %s
        LEFT JOIN offers o ON o.product_id = p.id
        WHERE r.id = 1
        """
                .formatted(selection));

    if (!ids.isEmpty()) {
      query = query.param("ids", ids.stream().map(ProductId::value).toList());
    }

    var rows =
        query
            .query(
                (rs, rowNumber) -> {
                  String revision = rs.getString("revision");
                  String id = rs.getString("id");
                  if (id == null) {
                    return new CatalogRow(revision, null, null);
                  }

                  var productId = new ProductId(id);
                  var product =
                      new Product(productId, rs.getString("name"), rs.getBigDecimal("unit_price"));
                  Integer quantity = rs.getObject("bundle_quantity", Integer.class);
                  var offer =
                      quantity == null
                          ? null
                          : new QuantityOffer(
                              productId, quantity, rs.getBigDecimal("bundle_price"));
                  return new CatalogRow(revision, product, offer);
                })
            .list();

    if (rows.isEmpty()) {
      throw new IllegalStateException("Catalog revision is missing");
    }

    var products = new ArrayList<Product>();
    var offers = new ArrayList<QuantityOffer>();
    for (CatalogRow row : rows) {
      if (row.product() != null) {
        products.add(row.product());
      }
      if (row.offer() != null) {
        offers.add(row.offer());
      }
    }

    return new CatalogSnapshot(products, offers, rows.getFirst().revision());
  }

  private record CatalogRow(String revision, Product product, QuantityOffer offer) {}
}
