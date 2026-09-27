package com.example.supermarket.catalog.configuration;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.CatalogSnapshot;
import com.example.supermarket.catalog.domain.Product;
import com.example.supermarket.catalog.domain.ProductId;
import com.example.supermarket.catalog.domain.QuantityOffer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@org.springframework.context.annotation.Profile("config-catalog")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CatalogProperties.class)
public class CatalogConfiguration {

  @Bean
  CatalogProvider catalogProvider(CatalogProperties properties) {
    var products =
        properties.products().stream()
            .map(
                product ->
                    new Product(new ProductId(product.id()), product.name(), product.unitPrice()))
            .toList();

    var offers =
        properties.offers().stream()
            .map(
                offer ->
                    new QuantityOffer(
                        new ProductId(offer.productId()), offer.quantity(), offer.bundlePrice()))
            .toList();

    return new ConfiguredCatalogProvider(new CatalogSnapshot(products, offers));
  }
}
