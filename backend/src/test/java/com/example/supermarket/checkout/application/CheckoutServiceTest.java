package com.example.supermarket.checkout.application;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.*;
import com.example.supermarket.checkout.domain.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {
  private final ProductId apple = new ProductId("APPLE");
  private final ProductId banana = new ProductId("BANANA");
  private final CatalogSnapshot catalog =
      new CatalogSnapshot(
          List.of(
              new Product(apple, "Apple", new BigDecimal("0.30")),
              new Product(banana, "Banana", new BigDecimal("0.50"))),
          List.of(new QuantityOffer(apple, 2, new BigDecimal("0.45"))));

  private static class RecordingProvider implements CatalogProvider {
    private final CatalogSnapshot snapshot;
    private int calls;
    private Set<ProductId> requested;

    RecordingProvider(CatalogSnapshot snapshot) {
      this.snapshot = snapshot;
    }

    public CatalogSnapshot findAll() {
      throw new AssertionError("Checkout must use the batch lookup");
    }

    public CatalogSnapshot findFor(Set<ProductId> ids) {
      calls++;
      requested = Set.copyOf(ids);
      return snapshot;
    }
  }

  @Test
  void retrievesDistinctProductsOnceAndPricesAllEntries() {
    var provider = new RecordingProvider(catalog);
    var service = new CheckoutService(provider, new PricingCalculator());
    var command =
        new CheckoutCommand(
            List.of(new CartItem(apple, 1), new CartItem(banana, 1), new CartItem(apple, 2)));

    var result = service.checkout(command);
    Receipt receipt = result.receipt();
    assertThat(result.catalogRevision()).isEqualTo(catalog.revision());

    assertThat(provider.calls).isEqualTo(1);
    assertThat(provider.requested).containsExactlyInAnyOrder(apple, banana);
    assertThat(receipt.total()).isEqualTo(new BigDecimal("1.25"));
    assertThat(receipt.items()).extracting(ReceiptLine::quantity).containsExactly(3, 1);
  }

  @Test
  void rejectsWholeCheckoutWhenTheBatchIsMissingAProduct() {
    var provider =
        new RecordingProvider(
            new CatalogSnapshot(List.of(catalog.products().get(apple)), List.of()));
    var service = new CheckoutService(provider, new PricingCalculator());

    assertThatThrownBy(
            () ->
                service.checkout(
                    new CheckoutCommand(List.of(new CartItem(apple, 2), new CartItem(banana, 1)))))
        .isInstanceOf(UnknownProductException.class)
        .hasMessageContaining("BANANA");
    assertThat(provider.calls).isEqualTo(1);
  }

  @Test
  void emptyCheckoutUsesOneEmptyLookupAndReturnsZero() {
    var provider = new RecordingProvider(new CatalogSnapshot(List.of(), List.of()));
    Receipt receipt =
        new CheckoutService(provider, new PricingCalculator())
            .checkout(new CheckoutCommand(List.of()))
            .receipt();

    assertThat(provider.calls).isEqualTo(1);
    assertThat(provider.requested).isEmpty();
    assertThat(receipt.items()).isEmpty();
    assertThat(receipt.total()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  void commandCopiesItsInput() {
    var items = new ArrayList<>(List.of(new CartItem(apple, 1)));
    var command = new CheckoutCommand(items);
    items.clear();

    assertThat(command.items()).containsExactly(new CartItem(apple, 1));
    assertThatThrownBy(() -> command.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void commandRejectsMissingItemsAndNullEntries() {
    assertThatThrownBy(() -> new CheckoutCommand(null)).isInstanceOf(InvalidCartException.class);
    assertThatThrownBy(() -> new CheckoutCommand(Arrays.asList((CartItem) null)))
        .isInstanceOf(InvalidCartException.class);
  }

  @Test
  void rejectsMissingCommandWithoutCallingProvider() {
    var provider = new RecordingProvider(catalog);

    assertThatThrownBy(() -> new CheckoutService(provider, new PricingCalculator()).checkout(null))
        .isInstanceOf(InvalidCartException.class);
    assertThat(provider.calls).isZero();
  }
}
