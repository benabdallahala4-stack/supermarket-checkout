package com.example.supermarket.catalog.persistence;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.SupermarketApplication;
import com.example.supermarket.catalog.application.port.CatalogEditor;
import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("database")
@Testcontainers
class CatalogManagementRestartTest {
  @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16.15");

  @Test
  void committedReplacementAndIntentionalEmptyCatalogSurviveApplicationRestarts() {
    String revision;
    var coffee = new ProductId("COFFEE");
    var replacement =
        new CatalogSnapshot(
            List.of(new Product(coffee, "Coffee", new BigDecimal("4.00"))),
            List.of(new QuantityOffer(coffee, 3, new BigDecimal("10.00"))));
    try (var context = start()) {
      var before = context.getBean(CatalogProvider.class).findAll();
      revision =
          context.getBean(CatalogEditor.class).replace(replacement, before.revision()).revision();
    }
    try (var context = start()) {
      var stored = context.getBean(CatalogProvider.class).findAll();
      assertThat(stored.products()).isEqualTo(replacement.products());
      assertThat(stored.offers()).isEqualTo(replacement.offers());
      assertThat(stored.revision()).isEqualTo(revision);
      revision =
          context
              .getBean(CatalogEditor.class)
              .replace(new CatalogSnapshot(List.of(), List.of()), revision)
              .revision();
    }
    try (var context = start()) {
      var stored = context.getBean(CatalogProvider.class).findAll();
      assertThat(stored.products()).isEmpty();
      assertThat(stored.offers()).isEmpty();
      assertThat(stored.revision()).isEqualTo(revision);
    }
  }

  private ConfigurableApplicationContext start() {
    return new SpringApplicationBuilder(SupermarketApplication.class)
        .run(
            "--server.port=0",
            "--spring.profiles.active=catalog-management",
            "--checkout.catalog-management.token=restart-test-only-0123456789abcdef",
            "--spring.datasource.url=" + DB.getJdbcUrl(),
            "--spring.datasource.username=" + DB.getUsername(),
            "--spring.datasource.password=" + DB.getPassword());
  }
}
