package com.example.supermarket.catalog.api;

import com.example.supermarket.catalog.domain.Product;
import com.example.supermarket.catalog.domain.QuantityOffer;
import com.example.supermarket.generated.model.Currency;
import com.example.supermarket.generated.model.ProductCatalog;
import com.example.supermarket.support.api.RestValueMapper;
import java.util.List;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = RestValueMapper.class)
public interface ProductRestMapper {
  @Mapping(target = "id", source = "product.id")
  @Mapping(target = "name", source = "product.name")
  @Mapping(target = "unitPrice", source = "product.unitPrice")
  @Mapping(target = "offer", source = "offer")
  com.example.supermarket.generated.model.Product toRest(Product product, QuantityOffer offer);

  @Mapping(target = "price", source = "bundlePrice")
  com.example.supermarket.generated.model.QuantityOffer toRest(QuantityOffer offer);

  default ProductCatalog toCatalog(List<com.example.supermarket.generated.model.Product> items) {
    return new ProductCatalog(Currency.EUR, items, items.size());
  }
}
