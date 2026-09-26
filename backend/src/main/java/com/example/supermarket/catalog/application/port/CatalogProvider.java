package com.example.supermarket.catalog.application.port;

import com.example.supermarket.catalog.domain.CatalogSnapshot;
import com.example.supermarket.catalog.domain.ProductId;
import java.util.Set;

/** Reads products and their active offers as one immutable catalog view. */
public interface CatalogProvider {
  CatalogSnapshot findAll();

  /**
   * Returns only known requested IDs; absent IDs remain absent. An empty request returns an empty
   * view.
   */
  CatalogSnapshot findFor(Set<ProductId> ids);
}
