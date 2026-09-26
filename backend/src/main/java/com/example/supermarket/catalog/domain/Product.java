package com.example.supermarket.catalog.domain;

import java.math.BigDecimal;

public record Product(ProductId id, String name, BigDecimal unitPrice) {
  public Product {
    if (id == null) {
      throw new IllegalArgumentException("Product ID is required");
    }

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Product name must not be blank");
    }

    unitPrice = ExactCents.normalize(unitPrice);
  }
}
