package com.example.supermarket.catalog.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ExactCents {
  private ExactCents() {}

  static BigDecimal normalize(BigDecimal amount) {
    if (amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("Price must be non-negative");
    }

    try {
      return amount.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("Price must be an exact number of cents", exception);
    }
  }
}
