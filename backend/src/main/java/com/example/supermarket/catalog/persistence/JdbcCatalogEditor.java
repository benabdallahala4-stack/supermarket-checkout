package com.example.supermarket.catalog.persistence;

import com.example.supermarket.catalog.application.CatalogConflictException;
import com.example.supermarket.catalog.application.port.CatalogEditor;
import com.example.supermarket.catalog.domain.CatalogSnapshot;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@Profile("catalog-management & !config-catalog")
public class JdbcCatalogEditor implements CatalogEditor {
  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public JdbcCatalogEditor(JdbcClient jdbc, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(manager);
  }

  @Override
  public CatalogSnapshot replace(CatalogSnapshot replacement, String expectedRevision) {
    return transaction.execute(
        status -> {
          String revision = UUID.randomUUID().toString();
          // The conditional update locks the singleton row and serializes competing writers.
          int changed =
              jdbc.sql(
                      "UPDATE catalog_revision SET version = CAST(:next AS uuid) WHERE id = 1 AND version = CAST(:expected AS uuid)")
                  .param("next", revision)
                  .param("expected", expectedRevision)
                  .update();
          if (changed != 1) {
            throw new CatalogConflictException();
          }

          jdbc.sql("DELETE FROM products").update();
          for (var product : replacement.products().values()) {
            jdbc.sql("INSERT INTO products (id, name, unit_price) VALUES (:id, :name, :price)")
                .param("id", product.id().value())
                .param("name", product.name())
                .param("price", product.unitPrice())
                .update();
          }
          for (var offer : replacement.offers().values()) {
            jdbc.sql(
                    "INSERT INTO offers (product_id, bundle_quantity, bundle_price) VALUES (:id, :quantity, :price)")
                .param("id", offer.productId().value())
                .param("quantity", offer.quantity())
                .param("price", offer.bundlePrice())
                .update();
          }

          return new CatalogSnapshot(
              replacement.products().values(), replacement.offers().values(), revision);
        });
  }
}
