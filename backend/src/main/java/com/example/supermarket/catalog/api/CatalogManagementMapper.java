package com.example.supermarket.catalog.api;

import com.example.supermarket.catalog.domain.CatalogSnapshot;
import com.example.supermarket.catalog.domain.Product;
import com.example.supermarket.catalog.domain.QuantityOffer;
import com.example.supermarket.generated.model.ManagedCatalog;
import com.example.supermarket.support.api.RestValueMapper;
import java.util.ArrayList;
import java.util.Comparator;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = RestValueMapper.class)
public interface CatalogManagementMapper {
  Product toDomain(com.example.supermarket.generated.model.Product product);

  @Mapping(target = "productId", source = "id")
  @Mapping(target = "quantity", source = "offer.quantity")
  @Mapping(target = "bundlePrice", source = "offer.price")
  QuantityOffer toOffer(com.example.supermarket.generated.model.Product product);

  @Mapping(target = "id", source = "product.id")
  @Mapping(target = "name", source = "product.name")
  @Mapping(target = "unitPrice", source = "product.unitPrice")
  @Mapping(target = "offer", source = "offer")
  com.example.supermarket.generated.model.Product toRest(Product product, QuantityOffer offer);

  @Mapping(target = "price", source = "bundlePrice")
  com.example.supermarket.generated.model.QuantityOffer toRest(QuantityOffer offer);

  default CatalogSnapshot toSnapshot(ManagedCatalog request) {
    if (request.getItems() == null) {
      throw new IllegalArgumentException("Catalog items are required");
    }
    var products = new ArrayList<Product>();
    var offers = new ArrayList<QuantityOffer>();
    for (var item : request.getItems()) {
      if (item == null) {
        throw new IllegalArgumentException("Catalog item must not be null");
      }
      products.add(toDomain(item));
      if (item.getOffer() != null) {
        offers.add(toOffer(item));
      }
    }
    return new CatalogSnapshot(products, offers);
  }

  default ManagedCatalog toRest(CatalogSnapshot catalog) {
    var items =
        catalog.products().values().stream()
            .sorted(Comparator.comparing(product -> product.id().value()))
            .map(product -> toRest(product, catalog.offers().get(product.id())))
            .toList();
    return new ManagedCatalog(catalog.revision(), items);
  }
}
