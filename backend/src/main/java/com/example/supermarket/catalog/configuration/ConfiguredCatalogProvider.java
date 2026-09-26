package com.example.supermarket.catalog.configuration;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.CatalogSnapshot;
import com.example.supermarket.catalog.domain.Product;
import com.example.supermarket.catalog.domain.ProductId;
import com.example.supermarket.catalog.domain.QuantityOffer;
import java.util.ArrayList;
import java.util.Set;

public final class ConfiguredCatalogProvider implements CatalogProvider {
  private final CatalogSnapshot snapshot;

  public ConfiguredCatalogProvider(CatalogSnapshot snapshot) {
    if (snapshot == null) {
      throw new IllegalArgumentException("Catalog snapshot is required");
    }

    this.snapshot = snapshot;
  }

  @Override
  public CatalogSnapshot findAll() {
    return snapshot;
  }

  @Override
  public CatalogSnapshot findFor(Set<ProductId> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("Requested product IDs are required");
    }

    var products = new ArrayList<Product>();
    var offers = new ArrayList<QuantityOffer>();
    for (ProductId id : ids) {
      if (id == null) {
        throw new IllegalArgumentException("Requested product ID must not be null");
      }

      Product product = snapshot.products().get(id);
      if (product != null) {
        products.add(product);

        QuantityOffer offer = snapshot.offers().get(id);
        if (offer != null) {
          offers.add(offer);
        }
      }
    }

    return new CatalogSnapshot(products, offers);
  }
}
