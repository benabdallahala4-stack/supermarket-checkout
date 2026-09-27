package com.example.supermarket.catalog.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.supermarket.SupermarketApplication;
import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.ProductId;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("database")
@Testcontainers
class DatabaseApplicationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.15");

  @Test
  void defaultApplicationUsesDatabaseAndRestartsPreserveEditsAndEmptyCatalog() {
    String revision;
    try (var context = start()) {
      var provider = context.getBean(CatalogProvider.class);
      assertThat(provider).isInstanceOf(JdbcCatalogProvider.class);
      assertThat(provider.findAll().products()).hasSize(3);
      revision = provider.findAll().revision();
      context
          .getBean(JdbcClient.class)
          .sql("UPDATE products SET unit_price = 0.40 WHERE id = 'APPLE'")
          .update();
    }

    try (var context = start()) {
      var catalog = context.getBean(CatalogProvider.class).findAll();
      assertThat(catalog.products().get(new ProductId("APPLE")).unitPrice())
          .isEqualTo(new BigDecimal("0.40"));
      assertThat(catalog.revision()).isEqualTo(revision);
      context.getBean(JdbcClient.class).sql("DELETE FROM products").update();
    }

    try (var context = start()) {
      assertThat(context.getBean(CatalogProvider.class).findAll().products()).isEmpty();
    }
  }

  private ConfigurableApplicationContext start() {
    return new SpringApplicationBuilder(SupermarketApplication.class)
        .run(
            "--server.port=0",
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword());
  }
}
