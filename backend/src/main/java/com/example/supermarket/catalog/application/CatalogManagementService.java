package com.example.supermarket.catalog.application;

import com.example.supermarket.catalog.application.port.CatalogEditor;
import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.CatalogSnapshot;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("catalog-management & !config-catalog")
public class CatalogManagementService {
  private final CatalogProvider provider;
  private final CatalogEditor editor;

  public CatalogManagementService(CatalogProvider provider, CatalogEditor editor) {
    this.provider = provider;
    this.editor = editor;
  }

  public CatalogSnapshot read() {
    return provider.findAll();
  }

  public CatalogSnapshot replace(CatalogSnapshot replacement, String revision) {
    return editor.replace(replacement, revision);
  }
}
