package com.example.supermarket.checkout.domain;

import com.example.supermarket.catalog.domain.ProductId;

public final class UnknownProductException extends IllegalArgumentException {
  public UnknownProductException(ProductId productId) {
    super("Unknown product: " + productId.value());
  }
}
