package com.example.supermarket.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

public record Receipt(
    List<ReceiptLine> items, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
  public Receipt {
    items = List.copyOf(items);
  }
}
