package com.example.supermarket.catalog.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** An immutable catalog view with at most one discounted quantity offer per product. */
public final class CatalogSnapshot {
  private final Map<ProductId, Product> products;
  private final Map<ProductId, QuantityOffer> offers;

  public CatalogSnapshot(Collection<Product> products, Collection<QuantityOffer> offers) {
    if (products == null || offers == null) {
      throw new IllegalArgumentException("Catalog collections are required");
    }

    var productsById = new HashMap<ProductId, Product>();
    for (Product product : products) {
      if (product == null) {
        throw new IllegalArgumentException("Catalog product must not be null");
      }

      if (productsById.putIfAbsent(product.id(), product) != null) {
        throw new IllegalArgumentException("Duplicate product: " + product.id().value());
      }
    }

    var offersById = new HashMap<ProductId, QuantityOffer>();
    for (QuantityOffer offer : offers) {
      if (offer == null) {
        throw new IllegalArgumentException("Catalog offer must not be null");
      }

      Product product = productsById.get(offer.productId());
      if (product == null) {
        throw new IllegalArgumentException(
            "Offer references unknown product: " + offer.productId().value());
      }

      BigDecimal regularPrice = product.unitPrice().multiply(BigDecimal.valueOf(offer.quantity()));
      if (offer.bundlePrice().compareTo(regularPrice) >= 0) {
        throw new IllegalArgumentException("Offer must save money: " + offer.productId().value());
      }

      if (offersById.putIfAbsent(offer.productId(), offer) != null) {
        throw new IllegalArgumentException("Duplicate offer: " + offer.productId().value());
      }
    }

    this.products = Map.copyOf(productsById);
    this.offers = Map.copyOf(offersById);
  }

  public Map<ProductId, Product> products() {
    return products;
  }

  public Map<ProductId, QuantityOffer> offers() {
    return offers;
  }
}
