package com.example.supermarket.catalog.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogDomainTest {
  private final ProductId apple = new ProductId("APPLE");

  private Product product(String price) {
    return new Product(apple, "Apple", new BigDecimal(price));
  }

  private QuantityOffer offer(int quantity, String price) {
    return new QuantityOffer(apple, quantity, new BigDecimal(price));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void rejectsBlankIdentifiers(String value) {
    assertThatIllegalArgumentException().isThrownBy(() -> new ProductId(value));
  }

  @Test
  void preservesIdentifierCaseAndWhitespace() {
    assertThat(new ProductId(" apple ").value()).isEqualTo(" apple ");
    assertThat(new ProductId("apple")).isNotEqualTo(apple);
    assertThat(new ProductId("APPLE")).isEqualTo(apple);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void rejectsBlankProductNames(String name) {
    assertThatIllegalArgumentException().isThrownBy(() -> new Product(apple, name, BigDecimal.ONE));
  }

  @Test
  void rejectsMissingProductFields() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new Product(null, "Apple", BigDecimal.ONE));
    assertThatIllegalArgumentException().isThrownBy(() -> new Product(apple, "Apple", null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.01", "0.001", "1.234"})
  void rejectsInvalidProductPrices(String price) {
    assertThatIllegalArgumentException().isThrownBy(() -> product(price));
  }

  @Test
  void normalizesExactCentsWithoutRounding() {
    assertThat(product("0.300").unitPrice()).isEqualTo(new BigDecimal("0.30"));
    assertThat(product("0").unitPrice()).isEqualTo(new BigDecimal("0.00"));
    assertThat(product("1E+2").unitPrice()).isEqualTo(new BigDecimal("100.00"));
    assertThat(offer(3, "0.800").bundlePrice()).isEqualTo(new BigDecimal("0.80"));
    assertThat(offer(2, "0").bundlePrice()).isEqualTo(new BigDecimal("0.00"));
  }

  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 1})
  void rejectsOfferQuantitiesBelowTwo(int quantity) {
    assertThatIllegalArgumentException().isThrownBy(() -> offer(quantity, "0.80"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.01", "0.001"})
  void rejectsInvalidOfferPrices(String price) {
    assertThatIllegalArgumentException().isThrownBy(() -> offer(3, price));
  }

  @Test
  void rejectsMissingOfferFields() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new QuantityOffer(null, 3, BigDecimal.ONE));
    assertThatIllegalArgumentException().isThrownBy(() -> new QuantityOffer(apple, 3, null));
  }

  @Test
  void createsImmutableSnapshotDefensively() {
    var products = new ArrayList<>(List.of(product("0.30")));
    var offers = new ArrayList<>(List.of(offer(3, "0.80")));
    var snapshot = new CatalogSnapshot(products, offers);
    products.clear();
    offers.clear();
    assertThat(snapshot.products()).containsOnlyKeys(apple);
    assertThat(snapshot.products().get(apple).name()).isEqualTo("Apple");
    assertThat(snapshot.offers().get(apple).quantity()).isEqualTo(3);
    assertThatThrownBy(() -> snapshot.products().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> snapshot.offers().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void acceptsEmptyCatalogAndProductsWithoutOffers() {
    assertThat(new CatalogSnapshot(List.of(), List.of()).products()).isEmpty();
    assertThat(new CatalogSnapshot(List.of(product("0")), List.of()).offers()).isEmpty();
  }

  @Test
  void rejectsDuplicateProductsAndOffers() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new CatalogSnapshot(List.of(product("0.30"), product("0.40")), List.of()));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new CatalogSnapshot(
                    List.of(product("0.30")), List.of(offer(3, "0.80"), offer(4, "1.00"))));
  }

  @Test
  void rejectsUnknownOfferProduct() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CatalogSnapshot(List.of(), List.of(offer(3, "0.80"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.90", "1.00"})
  void rejectsOffersWithoutSavings(String price) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CatalogSnapshot(List.of(product("0.30")), List.of(offer(3, price))));
  }

  @Test
  void permitsFreeDiscountAndLargeQuantities() {
    assertThat(
            new CatalogSnapshot(List.of(product("0.30")), List.of(offer(Integer.MAX_VALUE, "0")))
                .offers())
        .hasSize(1);
  }

  @Test
  void rejectsNullCollectionsAndMembers() {
    assertThatIllegalArgumentException().isThrownBy(() -> new CatalogSnapshot(null, List.of()));
    assertThatIllegalArgumentException().isThrownBy(() -> new CatalogSnapshot(List.of(), null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CatalogSnapshot(Arrays.asList((Product) null), List.of()));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CatalogSnapshot(List.of(), Arrays.asList((QuantityOffer) null)));
  }
}
