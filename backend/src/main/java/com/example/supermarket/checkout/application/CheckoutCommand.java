package com.example.supermarket.checkout.application;

import com.example.supermarket.checkout.domain.CartItem;
import com.example.supermarket.checkout.domain.InvalidCartException;
import java.util.List;
import java.util.Objects;

public record CheckoutCommand(List<CartItem> items) {
  public CheckoutCommand {
    if (items == null || items.stream().anyMatch(Objects::isNull)) {
      throw new InvalidCartException("Cart items must be present and non-null");
    }

    items = List.copyOf(items);
  }
}
