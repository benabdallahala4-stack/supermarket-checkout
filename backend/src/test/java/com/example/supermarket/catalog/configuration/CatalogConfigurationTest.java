package com.example.supermarket.catalog.configuration;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.ProductId;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CatalogConfigurationTest {
  private static Map<String, String> validProperties() {
    var properties = new LinkedHashMap<String, String>();
    properties.put("checkout.catalog.products[0].id", "APPLE");
    properties.put("checkout.catalog.products[0].name", "Apple");
    properties.put("checkout.catalog.products[0].unit-price", "0.30");
    properties.put("checkout.catalog.products[1].id", "BANANA");
    properties.put("checkout.catalog.products[1].name", "Banana");
    properties.put("checkout.catalog.products[1].unit-price", "0.50");
    properties.put("checkout.catalog.offers[0].product-id", "APPLE");
    properties.put("checkout.catalog.offers[0].quantity", "2");
    properties.put("checkout.catalog.offers[0].bundle-price", "0.45");
    return properties;
  }

  private ApplicationContextRunner context(Map<String, String> properties) {
    return new ApplicationContextRunner()
        .withUserConfiguration(CatalogConfiguration.class)
        .withPropertyValues(
            properties.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new));
  }

  @Test
  void bindsValidProductsAndOffers() {
    context(validProperties())
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasSingleBean(CatalogProvider.class);
              var catalog = context.getBean(CatalogProvider.class).findAll();

              assertThat(catalog.products()).hasSize(2);
              assertThat(catalog.products().get(new ProductId("APPLE")).unitPrice())
                  .isEqualTo(new BigDecimal("0.30"));
              assertThat(catalog.offers().get(new ProductId("APPLE")).bundlePrice())
                  .isEqualTo(new BigDecimal("0.45"));
            });
  }

  @Test
  void acceptsCatalogWithoutOffers() {
    var properties = validProperties();
    properties.keySet().removeIf(key -> key.startsWith("checkout.catalog.offers"));

    context(properties)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(CatalogProvider.class).findAll().offers()).isEmpty();
            });
  }

  @Test
  void acceptsExactTrailingZeroPricesAndFreeBundles() {
    var properties = validProperties();
    properties.put("checkout.catalog.products[0].unit-price", "0.300");
    properties.put("checkout.catalog.offers[0].bundle-price", "0");

    context(properties)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              var catalog = context.getBean(CatalogProvider.class).findAll();
              assertThat(catalog.products().get(new ProductId("APPLE")).unitPrice())
                  .isEqualTo(new BigDecimal("0.30"));
              assertThat(catalog.offers().get(new ProductId("APPLE")).bundlePrice())
                  .isEqualTo(new BigDecimal("0.00"));
            });
  }

  static Stream<Arguments> invalidValues() {
    return Stream.of(
        Arguments.of("duplicate products", "products[1].id", "APPLE"),
        Arguments.of("unknown offer product", "offers[0].product-id", "MISSING"),
        Arguments.of("negative price", "products[0].unit-price", "-0.30"),
        Arguments.of("fractional cents", "products[0].unit-price", "0.301"),
        Arguments.of("malformed price", "products[0].unit-price", "not-money"),
        Arguments.of("blank ID", "products[0].id", " "),
        Arguments.of("blank name", "products[0].name", " "),
        Arguments.of("negative offer price", "offers[0].bundle-price", "-0.01"),
        Arguments.of("fractional offer cents", "offers[0].bundle-price", "0.451"),
        Arguments.of("zero quantity", "offers[0].quantity", "0"),
        Arguments.of("single unit offer", "offers[0].quantity", "1"),
        Arguments.of("fractional quantity", "offers[0].quantity", "2.5"),
        Arguments.of("overflow quantity", "offers[0].quantity", "2147483648"),
        Arguments.of("no saving", "offers[0].bundle-price", "0.60"),
        Arguments.of("more expensive", "offers[0].bundle-price", "0.70"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidValues")
  void rejectsInvalidConfiguration(String description, String field, String value) {
    var properties = validProperties();
    properties.put("checkout.catalog." + field, value);

    context(properties).run(context -> assertThat(context).hasFailed());
  }

  static Stream<String> requiredFields() {
    return Stream.of(
        "products[0].id",
        "products[0].name",
        "products[0].unit-price",
        "offers[0].product-id",
        "offers[0].quantity",
        "offers[0].bundle-price");
  }

  @ParameterizedTest
  @MethodSource("requiredFields")
  void rejectsMissingRequiredFields(String field) {
    var properties = validProperties();
    properties.remove("checkout.catalog." + field);

    context(properties).run(context -> assertThat(context).hasFailed());
  }

  @Test
  void rejectsMissingCatalog() {
    context(Map.of()).run(context -> assertThat(context).hasFailed());
  }

  @Test
  void rejectsDuplicateOffers() {
    var properties = validProperties();
    properties.put("checkout.catalog.offers[1].product-id", "APPLE");
    properties.put("checkout.catalog.offers[1].quantity", "3");
    properties.put("checkout.catalog.offers[1].bundle-price", "0.75");

    context(properties).run(context -> assertThat(context).hasFailed());
  }

  @Test
  void externalYamlReplacesListsAndCanClearOffers(@TempDir Path directory) throws Exception {
    var external = directory.resolve("catalog.yml");
    Files.writeString(
        external,
        """
        checkout:
          catalog:
            products:
              - id: KIWI
                name: Kiwi
                unit-price: "1.25"
            offers: []
        """);

    context(Map.of("spring.config.additional-location", external.toUri().toString()))
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              var catalog = context.getBean(CatalogProvider.class).findAll();

              assertThat(catalog.products()).containsOnlyKeys(new ProductId("KIWI"));
              assertThat(catalog.products().get(new ProductId("KIWI")).unitPrice())
                  .isEqualTo(new BigDecimal("1.25"));
              assertThat(catalog.offers()).isEmpty();
            });
  }
}
