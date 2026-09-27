package com.example.supermarket.catalog.persistence;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("database")
@Testcontainers
class CatalogMigrationTest {
  @Container
  static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16.15");

  @Test
  void createsSchemaAndInitialDemoCatalogOnlyOnce() {
    var flyway =
        Flyway.configure()
            .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
            .load();

    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
    var jdbc =
        JdbcClient.create(
            new DriverManagerDataSource(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword()));
    assertThat(jdbc.sql("select count(*) from products").query(Integer.class).single())
        .isEqualTo(3);
    jdbc.sql("update products set name = 'Updated apple' where id = 'APPLE'").update();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(
            jdbc.sql("select name from products where id = 'APPLE'").query(String.class).single())
        .isEqualTo("Updated apple");

    jdbc.sql("delete from products").update();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(jdbc.sql("select count(*) from products").query(Integer.class).single()).isZero();
  }
}
