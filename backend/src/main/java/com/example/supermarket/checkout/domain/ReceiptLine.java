package com.example.supermarket.checkout.domain;

import com.example.supermarket.catalog.domain.ProductId;
import java.math.BigDecimal;
import java.util.Optional;

public record ReceiptLine(
    ProductId productId,
    String name,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    Optional<AppliedOffer> appliedOffer) {}
