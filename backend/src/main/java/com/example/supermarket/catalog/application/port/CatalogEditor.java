package com.example.supermarket.catalog.application.port;

import com.example.supermarket.catalog.domain.CatalogSnapshot;

public interface CatalogEditor {
  CatalogSnapshot replace(CatalogSnapshot replacement, String expectedRevision);
}
