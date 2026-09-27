package com.example.supermarket.catalog.persistence;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.domain.ProductId;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("database")
@Testcontainers
class JdbcCatalogProviderTest {
  @Container
  static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16.15");

  private final ProductId coffee = new ProductId("COFFEE");
  private DriverManagerDataSource dataSource;
  private CountingDataSource counting;
  private JdbcClient jdbc;
  private JdbcCatalogProvider provider;

  @BeforeEach
  void prepareCatalog() {
    dataSource =
        new DriverManagerDataSource(
            DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
    Flyway.configure().dataSource(dataSource).load().migrate();
    jdbc = JdbcClient.create(dataSource);
    jdbc.sql("delete from products").update();
    jdbc.sql(
            "insert into products values ('COFFEE', 'Coffee', 4.00), ('MILK', 'Milk', 1.10), ('FREE', 'Free sample', 0)")
        .update();
    jdbc.sql("insert into offers values ('COFFEE', 3, 10.00)").update();
    counting = new CountingDataSource(dataSource);
    provider = new JdbcCatalogProvider(JdbcClient.create(counting));
  }

  @Test
  void readsRequestedProductsAndOffersInOneStatementAndOmitsMissingIds() {
    var snapshot =
        provider.findFor(Set.of(coffee, new ProductId("MILK"), new ProductId("MISSING")));

    assertThat(counting.statements).isEqualTo(1);
    assertThat(snapshot.products()).containsOnlyKeys(coffee, new ProductId("MILK"));
    assertThat(snapshot.products().get(coffee).name()).isEqualTo("Coffee");
    assertThat(snapshot.products().get(coffee).unitPrice()).isEqualTo(new BigDecimal("4.00"));
    assertThat(snapshot.offers().get(coffee).quantity()).isEqualTo(3);
    assertThat(snapshot.offers().get(coffee).bundlePrice()).isEqualTo(new BigDecimal("10.00"));
    assertThat(snapshot.revision())
        .isEqualTo(
            jdbc.sql("select version::text from catalog_revision").query(String.class).single());
  }

  @Test
  void returnsAllProductsIncludingFreeProductsWithoutOffers() {
    var snapshot = provider.findAll();

    assertThat(counting.statements).isEqualTo(1);
    assertThat(snapshot.products()).hasSize(3);
    assertThat(snapshot.products().get(new ProductId("FREE")).unitPrice())
        .isEqualTo(new BigDecimal("0.00"));
    assertThat(snapshot.offers()).containsOnlyKeys(coffee);
  }

  @Test
  void emptySelectionsAndMissingProductsRetainTheSameGlobalRevision() {
    var revision = provider.findAll().revision();
    counting.statements = 0;
    var empty = provider.findFor(Set.of());
    assertThat(counting.statements).isEqualTo(1);
    assertThat(empty.products()).isEmpty();
    assertThat(empty.offers()).isEmpty();
    assertThat(empty.revision()).isEqualTo(revision);
    assertThat(provider.findFor(Set.of(new ProductId("MISSING"))).revision()).isEqualTo(revision);

    jdbc.sql("delete from products").update();
    var emptyCatalog = provider.findAll();
    assertThat(emptyCatalog.products()).isEmpty();
    assertThat(emptyCatalog.revision()).isEqualTo(revision);
  }

  @Test
  void readsLargeBatchesWithoutPerProductQueries() {
    var ids = new HashSet<ProductId>();
    for (int index = 0; index < 50; index++) {
      String id = "PRODUCT-" + index;
      jdbc.sql("insert into products values (:id, :name, 1.25)")
          .param("id", id)
          .param("name", id)
          .update();
      ids.add(new ProductId(id));
    }

    assertThat(provider.findFor(ids).products()).hasSize(50);
    assertThat(counting.statements).isEqualTo(1);
  }

  @Test
  void preservesAmountsBeyondBigintCentsWithoutRounding() {
    jdbc.sql("update products set unit_price = 100000000000000000000.01 where id = 'MILK'")
        .update();
    assertThat(
            provider
                .findFor(Set.of(new ProductId("MILK")))
                .products()
                .get(new ProductId("MILK"))
                .unitPrice())
        .isEqualTo(new BigDecimal("100000000000000000000.01"));
  }

  @Test
  void readsOldOrNewCommittedPricesOffersAndRevisionTogether() throws Exception {
    var before = provider.findAll();
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (var statement = connection.createStatement()) {
        statement.executeUpdate("update products set unit_price = 5.00 where id = 'COFFEE'");
        statement.executeUpdate(
            "update offers set bundle_price = 12.00 where product_id = 'COFFEE'");
        statement.executeUpdate("update catalog_revision set version = gen_random_uuid()");
      }
      var during = provider.findFor(Set.of(coffee));
      assertThat(during.revision()).isEqualTo(before.revision());
      assertThat(during.products().get(coffee).unitPrice()).isEqualTo(new BigDecimal("4.00"));
      assertThat(during.offers().get(coffee).bundlePrice()).isEqualTo(new BigDecimal("10.00"));
      connection.commit();
    }

    var after = provider.findFor(Set.of(coffee));
    assertThat(after.revision()).isNotEqualTo(before.revision());
    assertThat(after.products().get(coffee).unitPrice()).isEqualTo(new BigDecimal("5.00"));
    assertThat(after.offers().get(coffee).bundlePrice()).isEqualTo(new BigDecimal("12.00"));
  }

  @Test
  void rejectsNullSelectionsBeforeQuerying() {
    assertThatIllegalArgumentException().isThrownBy(() -> provider.findFor(null));
    var ids = new HashSet<ProductId>();
    ids.add(null);
    assertThatIllegalArgumentException().isThrownBy(() -> provider.findFor(ids));
    assertThat(counting.statements).isZero();
  }

  @Test
  void rejectsInvalidCrossRowOfferDataInsteadOfSilentlyPricingIt() {
    jdbc.sql("update offers set bundle_price = 12.00 where product_id = 'COFFEE'").update();
    assertThatIllegalArgumentException()
        .isThrownBy(provider::findAll)
        .withMessageContaining("Offer must save money");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "insert into products values ('NEG', 'Negative', -0.01)",
        "insert into products values ('FRACTION', 'Fraction', 0.001)",
        "insert into products values ('NAN', 'Not a number', 'NaN')",
        "insert into products values ('INF', 'Infinity', 'Infinity')",
        "insert into products values (' ', 'Blank ID', 1.00)",
        "insert into products values ('BLANK', ' ', 1.00)",
        "insert into products values ('COFFEE', 'Duplicate', 1.00)",
        "insert into offers values ('MILK', 1, 0.50)",
        "insert into offers values ('MILK', 2, -0.01)",
        "insert into offers values ('MILK', 2, 0.001)",
        "insert into offers values ('MISSING', 2, 0.50)"
      })
  void schemaRejectsInvalidRows(String sql) {
    assertThatThrownBy(() -> jdbc.sql(sql).update())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private static final class CountingDataSource extends DelegatingDataSource {
    private int statements;

    private CountingDataSource(DriverManagerDataSource delegate) {
      super(delegate);
    }

    @Override
    public Connection getConnection() throws SQLException {
      Connection connection = super.getConnection();
      return (Connection)
          Proxy.newProxyInstance(
              Connection.class.getClassLoader(),
              new Class<?>[] {Connection.class},
              (proxy, method, args) -> {
                if (method.getName().equals("prepareStatement")
                    || method.getName().equals("createStatement")) {
                  statements++;
                }
                try {
                  return method.invoke(connection, args);
                } catch (InvocationTargetException exception) {
                  throw exception.getCause();
                }
              });
    }
  }
}
