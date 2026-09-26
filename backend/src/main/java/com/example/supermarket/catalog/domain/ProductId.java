package com.example.supermarket.catalog.domain;

public record ProductId(String value) {
  public ProductId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Product ID must not be blank");
    }
  }
}
