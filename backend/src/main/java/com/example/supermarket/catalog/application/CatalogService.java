package com.example.supermarket.catalog.application;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.CatalogSnapshot;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
  private final CatalogProvider provider;

  public CatalogService(CatalogProvider provider) {
    this.provider = provider;
  }

  public CatalogSnapshot getCatalog() {
    return provider.findAll();
  }
}
