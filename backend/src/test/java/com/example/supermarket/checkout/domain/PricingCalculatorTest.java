package com.example.supermarket.checkout.domain;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.domain.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {
  private final PricingCalculator calculator = new PricingCalculator();
  private final ProductId apple = new ProductId("APPLE");
  private final ProductId banana = new ProductId("BANANA");
  private final ProductId orange = new ProductId("ORANGE");
  private final CatalogSnapshot catalog =
      new CatalogSnapshot(
          List.of(
              new Product(apple, "Apple", money("0.30")),
              new Product(banana, "Banana", money("0.50")),
              new Product(orange, "Orange", money("0.80"))),
          List.of(
              new QuantityOffer(apple, 2, money("0.45")),
              new QuantityOffer(banana, 3, money("1.20"))));

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  private Receipt apples(int quantity) {
    return calculator.calculate(List.of(new CartItem(apple, quantity)), catalog);
  }

  @Test
  void twoApplesCost45Cents() {
    assertThat(apples(2).total()).isEqualTo(money("0.45"));
  }

  @Test
  void threeApplesCost75Cents() {
    assertThat(apples(3).total()).isEqualTo(money("0.75"));
  }

  @Test
  void fiveApplesCost120Cents() {
    assertThat(apples(5).total()).isEqualTo(money("1.20"));
  }

  @ParameterizedTest
  @CsvSource({"1,0.30,0", "2,0.45,1", "3,0.75,1", "4,0.90,2", "5,1.20,2"})
  void appliesRepeatedBundlesAndRemainders(int quantity, String total, int applications) {
    Receipt receipt = apples(quantity);
    ReceiptLine line = receipt.items().getFirst();

    assertThat(receipt.items()).hasSize(1);
    assertThat(line.productId()).isEqualTo(apple);
    assertThat(line.name()).isEqualTo("Apple");
    assertThat(line.quantity()).isEqualTo(quantity);
    assertThat(line.unitPrice()).isEqualTo(money("0.30"));
    assertThat(line.subtotal()).isEqualTo(money("0.30").multiply(BigDecimal.valueOf(quantity)));
    assertThat(line.total()).isEqualTo(money(total));
    assertThat(line.discount()).isEqualTo(line.subtotal().subtract(line.total()));
    assertThat(receipt.subtotal()).isEqualTo(line.subtotal());
    assertThat(receipt.discount()).isEqualTo(line.discount());

    if (applications == 0) {
      assertThat(line.appliedOffer()).isEmpty();
    } else {
      assertThat(line.appliedOffer()).contains(new AppliedOffer(2, money("0.45"), applications));
    }
  }

  @Test
  void appliesIndependentOffersToMixedProducts() {
    Receipt receipt =
        calculator.calculate(List.of(new CartItem(banana, 4), new CartItem(apple, 3)), catalog);

    assertThat(receipt.items()).extracting(ReceiptLine::productId).containsExactly(apple, banana);
    assertThat(receipt.subtotal()).isEqualTo(money("2.90"));
    assertThat(receipt.discount()).isEqualTo(money("0.45"));
    assertThat(receipt.total()).isEqualTo(money("2.45"));
    assertThat(receipt.items().get(1).appliedOffer())
        .contains(new AppliedOffer(3, money("1.20"), 1));
  }

  @Test
  void chargesRegularPriceWithoutOffer() {
    Receipt receipt = calculator.calculate(List.of(new CartItem(orange, 3)), catalog);

    assertThat(receipt.total()).isEqualTo(money("2.40"));
    assertThat(receipt.discount()).isEqualTo(money("0.00"));
    assertThat(receipt.items().getFirst().appliedOffer()).isEmpty();
  }

  @Test
  void emptyCartHasNoLinesAndExactZeroTotals() {
    Receipt receipt = calculator.calculate(List.of(), new CatalogSnapshot(List.of(), List.of()));

    assertThat(receipt.items()).isEmpty();
    assertThat(receipt.subtotal()).isEqualTo(money("0.00"));
    assertThat(receipt.discount()).isEqualTo(money("0.00"));
    assertThat(receipt.total()).isEqualTo(money("0.00"));
  }

  @Test
  void combinesDuplicateEntriesBeforeApplyingOffersWithoutMutatingInput() {
    var items =
        new ArrayList<>(
            List.of(new CartItem(banana, 1), new CartItem(apple, 1), new CartItem(apple, 2)));
    var original = List.copyOf(items);
    Receipt receipt = calculator.calculate(items, catalog);

    assertThat(receipt)
        .isEqualTo(
            calculator.calculate(
                List.of(new CartItem(apple, 3), new CartItem(banana, 1)), catalog));
    Collections.reverse(items);
    assertThat(calculator.calculate(items, catalog)).isEqualTo(receipt);
    Collections.reverse(items);
    assertThat(items).isEqualTo(original);
  }

  @Test
  void rejectsAggregateOverflow() {
    assertThatThrownBy(
            () ->
                calculator.calculate(
                    List.of(new CartItem(apple, Integer.MAX_VALUE), new CartItem(apple, 1)),
                    catalog))
        .isInstanceOf(InvalidCartException.class)
        .hasMessageContaining("quantity");
  }

  @Test
  void supportsLargestValidAggregatedQuantityWithoutExpandingUnits() {
    Receipt receipt =
        calculator.calculate(
            List.of(new CartItem(apple, Integer.MAX_VALUE - 1), new CartItem(apple, 1)), catalog);

    assertThat(receipt.items().getFirst().quantity()).isEqualTo(Integer.MAX_VALUE);
    assertThat(receipt.subtotal()).isEqualTo(money("644245094.10"));
    assertThat(receipt.total()).isEqualTo(money("483183820.65"));
    assertThat(receipt.discount()).isEqualTo(money("161061273.45"));
  }

  @Test
  void rejectsUnknownProductEvenInMixedCart() {
    var missing = new ProductId("MISSING");

    assertThatThrownBy(
            () ->
                calculator.calculate(
                    List.of(new CartItem(apple, 2), new CartItem(missing, 1)), catalog))
        .isInstanceOf(UnknownProductException.class)
        .hasMessageContaining("MISSING");
    assertThat(apples(2).total()).isEqualTo(money("0.45"));
  }

  @Test
  void rejectsNullCartAndNullEntries() {
    assertThatThrownBy(() -> calculator.calculate(null, catalog))
        .isInstanceOf(InvalidCartException.class);
    assertThatThrownBy(() -> calculator.calculate(Arrays.asList((CartItem) null), catalog))
        .isInstanceOf(InvalidCartException.class);
  }

  @Test
  void rejectsMissingCatalog() {
    assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(List.of(), null));
  }

  @Test
  void handlesFreeProductsAndFreeBundles() {
    var freeCatalog =
        new CatalogSnapshot(
            List.of(
                new Product(apple, "Apple", money("0.30")),
                new Product(banana, "Banana", money("0.00"))),
            List.of(new QuantityOffer(apple, 2, money("0.00"))));
    Receipt receipt =
        calculator.calculate(List.of(new CartItem(apple, 3), new CartItem(banana, 5)), freeCatalog);

    assertThat(receipt.subtotal()).isEqualTo(money("0.90"));
    assertThat(receipt.discount()).isEqualTo(money("0.60"));
    assertThat(receipt.total()).isEqualTo(money("0.30"));
  }

  @Test
  void receiptDefensivelyCopiesItsLines() {
    Receipt calculated = apples(3);
    var lines = new ArrayList<>(calculated.items());
    Receipt receipt =
        new Receipt(lines, calculated.subtotal(), calculated.discount(), calculated.total());
    lines.clear();

    assertThat(receipt.items()).hasSize(1);
    assertThatThrownBy(() -> receipt.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void repeatedCallsUseOnlyTheirOwnCatalogAndCart() {
    var alternative =
        new CatalogSnapshot(List.of(new Product(apple, "Apple", money("2.00"))), List.of());

    assertThat(apples(3).total()).isEqualTo(money("0.75"));
    assertThat(calculator.calculate(List.of(new CartItem(apple, 3)), alternative).total())
        .isEqualTo(money("6.00"));
    assertThat(apples(3).total()).isEqualTo(money("0.75"));
  }

  @Test
  void concurrentCallsDoNotShareState() throws Exception {
    var barrier = new CyclicBarrier(4);
    var jobs = new ArrayList<Callable<Receipt>>();
    for (int i = 1; i <= 4; i++) {
      int quantity = i;
      jobs.add(
          () -> {
            barrier.await(10, TimeUnit.SECONDS);
            return apples(quantity);
          });
    }

    try (var executor = Executors.newFixedThreadPool(4)) {
      var results = executor.invokeAll(jobs, 15, TimeUnit.SECONDS);
      assertThat(results.get(0).get().total()).isEqualTo(money("0.30"));
      assertThat(results.get(1).get().total()).isEqualTo(money("0.45"));
      assertThat(results.get(2).get().total()).isEqualTo(money("0.75"));
      assertThat(results.get(3).get().total()).isEqualTo(money("0.90"));
    }
  }
}
