package com.example.supermarket.checkout.domain;

public final class InvalidCartException extends IllegalArgumentException {
  public InvalidCartException(String message) {
    super(message);
  }
}
