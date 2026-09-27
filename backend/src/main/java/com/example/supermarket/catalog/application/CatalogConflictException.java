package com.example.supermarket.catalog.application;

public class CatalogConflictException extends RuntimeException {
  public CatalogConflictException() {
    super("Catalog changed; read it again before replacing it");
  }
}
