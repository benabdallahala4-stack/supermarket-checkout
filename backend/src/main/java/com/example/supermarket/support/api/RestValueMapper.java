package com.example.supermarket.support.api;

import com.example.supermarket.catalog.domain.ProductId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class RestValueMapper {
  public String money(BigDecimal value) {
    return value == null ? null : value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
  }

  public String productId(ProductId value) {
    return value == null ? null : value.value();
  }

  public ProductId productId(String value) {
    return value == null ? null : new ProductId(value);
  }
}
