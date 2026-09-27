package com.example.supermarket.checkout.application;

import com.example.supermarket.checkout.domain.Receipt;

/** Pricing output and the catalog snapshot used to calculate it. */
public record CheckoutResult(Receipt receipt, String catalogRevision) {}
