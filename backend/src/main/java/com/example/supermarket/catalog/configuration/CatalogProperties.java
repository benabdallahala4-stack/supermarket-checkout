package com.example.supermarket.catalog.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "checkout.catalog")
public record CatalogProperties(
    @NotNull @Valid List<@NotNull ProductProperties> products,
    @Valid List<@NotNull OfferProperties> offers) {

  public CatalogProperties {
    products = products == null ? null : List.copyOf(products);
    offers = offers == null ? List.of() : List.copyOf(offers);
  }

  public record ProductProperties(
      @NotNull String id, @NotNull String name, @NotNull BigDecimal unitPrice) {}

  public record OfferProperties(
      @NotNull String productId, @NotNull Integer quantity, @NotNull BigDecimal bundlePrice) {}
}
