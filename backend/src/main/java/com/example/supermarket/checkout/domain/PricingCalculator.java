package com.example.supermarket.checkout.domain;

import com.example.supermarket.catalog.domain.CatalogSnapshot;
import com.example.supermarket.catalog.domain.Product;
import com.example.supermarket.catalog.domain.ProductId;
import com.example.supermarket.catalog.domain.QuantityOffer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Calculates receipts from one catalog view without retaining per-checkout state. */
public final class PricingCalculator {
  private static final BigDecimal ZERO = new BigDecimal("0.00");

  public Receipt calculate(List<CartItem> items, CatalogSnapshot catalog) {
    if (catalog == null) {
      throw new IllegalArgumentException("Catalog is required");
    }

    Map<ProductId, Integer> quantities = aggregateQuantities(items);
    var lines = new ArrayList<ReceiptLine>();
    BigDecimal subtotal = ZERO;
    BigDecimal discount = ZERO;
    BigDecimal total = ZERO;

    var orderedQuantities =
        quantities.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(ProductId::value)))
            .toList();

    for (var entry : orderedQuantities) {
      Product product = catalog.products().get(entry.getKey());
      if (product == null) {
        throw new UnknownProductException(entry.getKey());
      }

      QuantityOffer offer = catalog.offers().get(entry.getKey());
      ReceiptLine line = priceLine(product, entry.getValue(), offer);
      lines.add(line);

      subtotal = subtotal.add(line.subtotal());
      discount = discount.add(line.discount());
      total = total.add(line.total());
    }

    return new Receipt(lines, subtotal, discount, total);
  }

  private Map<ProductId, Integer> aggregateQuantities(List<CartItem> items) {
    if (items == null) {
      throw new InvalidCartException("Cart items are required");
    }

    var quantities = new HashMap<ProductId, Integer>();
    for (CartItem item : items) {
      if (item == null) {
        throw new InvalidCartException("Cart item must not be null");
      }

      try {
        quantities.merge(item.productId(), item.quantity(), Math::addExact);
      } catch (ArithmeticException exception) {
        throw new InvalidCartException(
            "Combined quantity exceeds the supported limit for " + item.productId().value());
      }
    }

    return quantities;
  }

  private ReceiptLine priceLine(Product product, int quantity, QuantityOffer offer) {
    BigDecimal subtotal = product.unitPrice().multiply(BigDecimal.valueOf(quantity));
    BigDecimal total = subtotal;
    Optional<AppliedOffer> appliedOffer = Optional.empty();

    if (offer != null && quantity >= offer.quantity()) {
      int applications = quantity / offer.quantity();
      int remainder = quantity % offer.quantity();
      BigDecimal bundleTotal = offer.bundlePrice().multiply(BigDecimal.valueOf(applications));
      BigDecimal remainderTotal = product.unitPrice().multiply(BigDecimal.valueOf(remainder));

      total = bundleTotal.add(remainderTotal);
      appliedOffer =
          Optional.of(new AppliedOffer(offer.quantity(), offer.bundlePrice(), applications));
    }

    return new ReceiptLine(
        product.id(),
        product.name(),
        quantity,
        product.unitPrice(),
        subtotal,
        subtotal.subtract(total),
        total,
        appliedOffer);
  }
}
