package com.example.supermarket.checkout.domain;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.domain.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import net.jqwik.api.*;

class PricingPropertiesTest {
  private final PricingCalculator calculator = new PricingCalculator();

  record PricingCase(CatalogSnapshot catalog, List<CartItem> items) {}

  @Provide
  Arbitrary<PricingCase> cases() {
    var prices = Arbitraries.integers().between(0, 1000).list().ofMinSize(1).ofMaxSize(8);
    var entries = Arbitraries.integers().between(0, 5000).list().ofMinSize(0).ofMaxSize(20);
    var bundleSize = Arbitraries.integers().between(2, 10);
    var savingsSeed = Arbitraries.integers().between(0, 10000);

    return Combinators.combine(prices, entries, bundleSize, savingsSeed)
        .as(
            (cents, encodedItems, size, savings) -> {
              var products = new ArrayList<Product>();
              var offers = new ArrayList<QuantityOffer>();
              for (int i = 0; i < cents.size(); i++) {
                ProductId id = new ProductId("P" + i);
                int price = cents.get(i);
                products.add(new Product(id, "Product " + i, BigDecimal.valueOf(price, 2)));

                if (i % 2 == 0 && price > 0) {
                  int regular = price * size;
                  int discount = savings % regular + 1;
                  offers.add(
                      new QuantityOffer(id, size, BigDecimal.valueOf(regular - discount, 2)));
                }
              }

              var items =
                  encodedItems.stream()
                      .map(
                          value ->
                              new CartItem(products.get(value % cents.size()).id(), value % 50 + 1))
                      .toList();
              return new PricingCase(new CatalogSnapshot(products, offers), items);
            });
  }

  @Property(tries = 300)
  void linesAndReceiptPreserveArithmetic(@ForAll("cases") PricingCase input) {
    Receipt receipt = calculator.calculate(input.items(), input.catalog());
    var quantities = new HashMap<ProductId, Integer>();
    for (CartItem item : input.items()) {
      quantities.merge(item.productId(), item.quantity(), Integer::sum);
    }

    assertThat(receipt.items()).hasSize(quantities.size());
    assertThat(receipt.items()).extracting(line -> line.productId().value()).isSorted();
    BigDecimal subtotal = new BigDecimal("0.00");
    BigDecimal discount = new BigDecimal("0.00");
    BigDecimal total = new BigDecimal("0.00");

    for (ReceiptLine line : receipt.items()) {
      assertThat(line.quantity()).isEqualTo(quantities.get(line.productId()));
      assertThat(line.subtotal())
          .isEqualByComparingTo(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
      assertThat(line.discount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
      assertThat(line.total()).isBetween(BigDecimal.ZERO, line.subtotal());
      assertThat(line.subtotal().subtract(line.discount())).isEqualByComparingTo(line.total());
      assertThat(line.unitPrice().scale()).isEqualTo(2);
      assertThat(line.subtotal().scale()).isEqualTo(2);
      assertThat(line.discount().scale()).isEqualTo(2);
      assertThat(line.total().scale()).isEqualTo(2);
      subtotal = subtotal.add(line.subtotal());
      discount = discount.add(line.discount());
      total = total.add(line.total());
    }

    assertThat(receipt.subtotal()).isEqualTo(subtotal);
    assertThat(receipt.discount()).isEqualTo(discount);
    assertThat(receipt.total()).isEqualTo(total);
    assertThat(receipt.subtotal().subtract(receipt.discount())).isEqualTo(receipt.total());
  }

  @Property(tries = 300)
  void inputPermutationDoesNotChangeReceipt(@ForAll("cases") PricingCase input) {
    var reversed = new ArrayList<>(input.items());
    Collections.reverse(reversed);

    assertThat(calculator.calculate(reversed, input.catalog()))
        .isEqualTo(calculator.calculate(input.items(), input.catalog()));
  }

  @Property(tries = 300)
  void splittingEntriesDoesNotChangeReceipt(@ForAll("cases") PricingCase input) {
    var split = new ArrayList<CartItem>();
    for (CartItem item : input.items()) {
      split.add(new CartItem(item.productId(), 1));
      if (item.quantity() > 1) {
        split.add(new CartItem(item.productId(), item.quantity() - 1));
      }
    }

    assertThat(calculator.calculate(split, input.catalog()))
        .isEqualTo(calculator.calculate(input.items(), input.catalog()));
  }
}
