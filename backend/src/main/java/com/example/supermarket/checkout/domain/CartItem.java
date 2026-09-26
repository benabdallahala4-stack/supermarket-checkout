package com.example.supermarket.checkout.domain;

import com.example.supermarket.catalog.domain.ProductId;

public record CartItem(ProductId productId, int quantity) {
  public CartItem {
    if (productId == null) {
      throw new InvalidCartException("Cart product ID is required");
    }

    if (quantity <= 0) {
      throw new InvalidCartException("Cart quantity must be positive");
    }
  }
}
