package com.example.supermarket.catalog.domain;

import java.math.BigDecimal;

public record QuantityOffer(ProductId productId, int quantity, BigDecimal bundlePrice) {
  public QuantityOffer {
    if (productId == null) {
      throw new IllegalArgumentException("Offer product ID is required");
    }

    if (quantity < 2) {
      throw new IllegalArgumentException("Offer quantity must be at least two");
    }

    bundlePrice = ExactCents.normalize(bundlePrice);
  }
}
