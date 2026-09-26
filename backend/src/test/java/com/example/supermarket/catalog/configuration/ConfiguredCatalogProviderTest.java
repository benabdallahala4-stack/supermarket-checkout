package com.example.supermarket.catalog.configuration;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.domain.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfiguredCatalogProviderTest {
  private final ProductId apple = new ProductId("APPLE");
  private final ProductId banana = new ProductId("BANANA");
  private final ProductId unknown = new ProductId("MISSING");
  private final Product product = new Product(apple, "Apple", new BigDecimal("0.30"));
  private final QuantityOffer offer = new QuantityOffer(apple, 2, new BigDecimal("0.45"));
  private final CatalogSnapshot catalog =
      new CatalogSnapshot(
          List.of(product, new Product(banana, "Banana", new BigDecimal("0.50"))), List.of(offer));
  private final ConfiguredCatalogProvider provider = new ConfiguredCatalogProvider(catalog);

  @Test
  void retrievesAllProductsAndOffers() {
    assertThat(provider.findAll().products()).containsOnlyKeys(apple, banana);
    assertThat(provider.findAll().offers()).containsEntry(apple, offer);
  }

  @Test
  void retrievesOnlyRequestedKnownProductsWithTheirOffers() {
    CatalogSnapshot subset = provider.findFor(Set.of(apple, unknown));

    assertThat(subset.products()).containsOnlyKeys(apple).containsEntry(apple, product);
    assertThat(subset.offers()).containsOnlyKeys(apple).containsEntry(apple, offer);
    assertThat(provider.findAll().products()).hasSize(2);
  }

  @Test
  void productWithoutOfferHasNoOfferInSubset() {
    CatalogSnapshot subset = provider.findFor(Set.of(banana));

    assertThat(subset.products()).containsOnlyKeys(banana);
    assertThat(subset.offers()).isEmpty();
  }

  @Test
  void emptyAndUnknownOnlyRequestsReturnEmptySnapshots() {
    for (Set<ProductId> ids : List.of(Set.<ProductId>of(), Set.of(unknown))) {
      assertThat(provider.findFor(ids).products()).isEmpty();
      assertThat(provider.findFor(ids).offers()).isEmpty();
    }
  }

  @Test
  void requestAndSourceMutationDoNotChangeReturnedSnapshots() {
    var products = new ArrayList<>(List.of(product));
    var offers = new ArrayList<>(List.of(offer));
    var provider = new ConfiguredCatalogProvider(new CatalogSnapshot(products, offers));
    var ids = new HashSet<>(Set.of(apple));
    CatalogSnapshot subset = provider.findFor(ids);
    ids.clear();
    products.clear();
    offers.clear();

    assertThat(subset.products()).containsOnlyKeys(apple);
    assertThat(subset.offers()).containsOnlyKeys(apple);
    assertThat(provider.findAll().products()).containsOnlyKeys(apple);
    assertThatThrownBy(() -> subset.products().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> subset.offers().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsNullRequestsAndNullIdentifiers() {
    assertThatIllegalArgumentException().isThrownBy(() -> provider.findFor(null));
    var ids = new HashSet<ProductId>();
    ids.add(null);
    assertThatIllegalArgumentException().isThrownBy(() -> provider.findFor(ids));
  }

  @Test
  void rejectsMissingSnapshot() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfiguredCatalogProvider(null));
  }
}
