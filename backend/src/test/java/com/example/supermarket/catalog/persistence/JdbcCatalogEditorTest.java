package com.example.supermarket.catalog.persistence;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.application.CatalogConflictException;
import com.example.supermarket.catalog.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("database")
@Testcontainers
class JdbcCatalogEditorTest {
  @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16.15");
  private JdbcClient jdbc;
  private JdbcCatalogProvider provider;
  private JdbcCatalogEditor editor;

  @BeforeEach
  void setup() {
    var ds = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
    Flyway.configure().dataSource(ds).load().migrate();
    jdbc = JdbcClient.create(ds);
    provider = new JdbcCatalogProvider(jdbc);
    editor = new JdbcCatalogEditor(jdbc, new DataSourceTransactionManager(ds));
  }

  private CatalogSnapshot catalog(String id) {
    var productId = new ProductId(id);
    return new CatalogSnapshot(
        List.of(new Product(productId, id, new BigDecimal("4.00"))),
        List.of(new QuantityOffer(productId, 3, new BigDecimal("10.00"))));
  }

  @Test
  void replacesAllRowsAndRevisionAndSupportsEmptyCatalog() {
    var before = provider.findAll();
    var after = editor.replace(catalog("COFFEE"), before.revision());
    assertThat(after.revision()).isNotEqualTo(before.revision());
    assertThat(provider.findAll().products()).containsOnlyKeys(new ProductId("COFFEE"));
    assertThat(provider.findAll().offers()).isEqualTo(after.offers());
    assertThat(provider.findAll().revision()).isEqualTo(after.revision());
    var empty = editor.replace(new CatalogSnapshot(List.of(), List.of()), after.revision());
    assertThat(empty.products()).isEmpty();
    assertThat(provider.findAll().products()).isEmpty();
    assertThat(empty.revision()).isNotEqualTo(after.revision());
  }

  @Test
  void rejectsStaleWriterWithoutChangingAnything() {
    var before = provider.findAll();
    var accepted = editor.replace(catalog("FIRST"), before.revision());
    assertThatThrownBy(() -> editor.replace(catalog("SECOND"), before.revision()))
        .isInstanceOf(CatalogConflictException.class);
    assertThat(provider.findAll().products()).isEqualTo(accepted.products());
    assertThat(provider.findAll().revision()).isEqualTo(accepted.revision());
  }

  @Test
  void rollsBackProductsOffersAndRevisionWhenInsertionFails() {
    var before = provider.findAll();
    // PostgreSQL text rejects NUL; the failure occurs after the replacement has started.
    assertThatThrownBy(() -> editor.replace(catalog("BAD\u0000ID"), before.revision()))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    var after = provider.findAll();
    assertThat(after.products()).isEqualTo(before.products());
    assertThat(after.offers()).isEqualTo(before.offers());
    assertThat(after.revision()).isEqualTo(before.revision());
  }

  @Test
  void simultaneousWritersWithSameRevisionHaveExactlyOneWinner() throws Exception {
    var revision = provider.findAll().revision();
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first = pool.submit(() -> attempt("ONE", revision, start));
      var second = pool.submit(() -> attempt("TWO", revision, start));
      start.countDown();
      assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
      assertThat(provider.findAll().products()).hasSize(1);
      assertThat(provider.findAll().offers()).hasSize(1);
    }
  }

  private boolean attempt(String id, String revision, CountDownLatch start) throws Exception {
    start.await();
    try {
      editor.replace(catalog(id), revision);
      return true;
    } catch (CatalogConflictException conflict) {
      return false;
    }
  }
}
