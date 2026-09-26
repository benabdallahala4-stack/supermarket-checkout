package com.example.supermarket.catalog.api;

import com.example.supermarket.catalog.application.CatalogService;
import com.example.supermarket.generated.api.ProductsApi;
import com.example.supermarket.generated.model.ProductCatalog;
import java.util.Comparator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {
  private final CatalogService service;
  private final ProductRestMapper mapper;

  public ProductController(CatalogService service, ProductRestMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  public ResponseEntity<ProductCatalog> getProducts() {
    var catalog = service.getCatalog();
    var items =
        catalog.products().values().stream()
            .sorted(Comparator.comparing(product -> product.id().value()))
            .map(product -> mapper.toRest(product, catalog.offers().get(product.id())))
            .toList();

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(mapper.toCatalog(items));
  }
}
