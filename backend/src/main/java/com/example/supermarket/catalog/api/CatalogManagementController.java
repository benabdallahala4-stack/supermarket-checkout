package com.example.supermarket.catalog.api;

import com.example.supermarket.catalog.application.CatalogManagementService;
import com.example.supermarket.generated.api.CatalogManagementApi;
import com.example.supermarket.generated.model.CatalogReplacement;
import com.example.supermarket.generated.model.ManagedCatalog;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("catalog-management & !config-catalog")
public class CatalogManagementController implements CatalogManagementApi {
  private final CatalogManagementService service;
  private final CatalogManagementMapper mapper;

  public CatalogManagementController(
      CatalogManagementService service, CatalogManagementMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  public ResponseEntity<ManagedCatalog> getManagedCatalog() {
    var catalog = service.read();

    return ResponseEntity.ok()
        .eTag(CatalogEtag.format(catalog.revision()))
        .body(mapper.toRest(catalog));
  }

  @Override
  public ResponseEntity<ManagedCatalog> replaceCatalog(
      String ifMatch, CatalogReplacement replacement) {
    var catalog = service.replace(mapper.toSnapshot(replacement), CatalogEtag.revision(ifMatch));

    return ResponseEntity.ok()
        .eTag(CatalogEtag.format(catalog.revision()))
        .body(mapper.toRest(catalog));
  }
}
