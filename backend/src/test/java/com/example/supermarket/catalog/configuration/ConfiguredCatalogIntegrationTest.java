package com.example.supermarket.catalog.configuration;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.application.CatalogService;
import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.ProductId;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles("config-catalog")
@SpringBootTest
class ConfiguredCatalogIntegrationTest {
  @Autowired private CatalogService service;
  @Autowired private CatalogProvider provider;

  @Test
  void loadsBundledYamlThroughApplicationService() {
    var catalog = service.getCatalog();
    var apple = new ProductId("APPLE");
    var banana = new ProductId("BANANA");
    var orange = new ProductId("ORANGE");

    assertThat(catalog.products()).containsOnlyKeys(apple, banana, orange);
    assertThat(catalog.products().get(apple).unitPrice()).isEqualTo(new BigDecimal("0.30"));
    assertThat(catalog.products().get(banana).unitPrice()).isEqualTo(new BigDecimal("0.50"));
    assertThat(catalog.products().get(orange).unitPrice()).isEqualTo(new BigDecimal("0.80"));
    assertThat(catalog.offers()).containsOnlyKeys(apple, banana);
    assertThat(catalog.offers().get(apple).quantity()).isEqualTo(2);
    assertThat(catalog.offers().get(apple).bundlePrice()).isEqualTo(new BigDecimal("0.45"));
    assertThat(catalog.offers().get(banana).quantity()).isEqualTo(3);
    assertThat(catalog.offers().get(banana).bundlePrice()).isEqualTo(new BigDecimal("1.20"));
    assertThat(catalog.products()).isEqualTo(provider.findAll().products());
  }
}
