package com.example.supermarket.checkout.domain;

import java.math.BigDecimal;

public record AppliedOffer(int quantity, BigDecimal bundlePrice, int applications) {}
