package com.example.supermarket.checkout.domain;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.domain.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CartItemTest {
  @ParameterizedTest
  @ValueSource(ints = {Integer.MIN_VALUE, -1, 0})
  void rejectsNonPositiveQuantities(int quantity) {
    assertThatThrownBy(() -> new CartItem(new ProductId("APPLE"), quantity))
        .isInstanceOf(InvalidCartException.class);
  }

  @Test
  void rejectsMissingProductId() {
    assertThatThrownBy(() -> new CartItem(null, 1)).isInstanceOf(InvalidCartException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, Integer.MAX_VALUE})
  void acceptsPositiveQuantities(int quantity) {
    var item = new CartItem(new ProductId("APPLE"), quantity);
    assertThat(item.productId()).isEqualTo(new ProductId("APPLE"));
    assertThat(item.quantity()).isEqualTo(quantity);
  }
}
