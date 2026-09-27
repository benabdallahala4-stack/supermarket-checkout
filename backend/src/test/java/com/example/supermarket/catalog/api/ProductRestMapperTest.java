package com.example.supermarket.catalog.api;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.domain.*;
import com.example.supermarket.generated.model.Currency;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles("config-catalog")
@SpringBootTest
class ProductRestMapperTest {
  @Autowired private ProductRestMapper mapper;
  private final ProductId id = new ProductId("APPLE");
  private final Product product = new Product(id, "Apple", new BigDecimal("0.30"));

  @Test
  void mapsProductAndOfferWithoutChangingTheirValues() {
    var result = mapper.toRest(product, new QuantityOffer(id, 2, new BigDecimal("0.45")));

    assertThat(result.getId()).isEqualTo("APPLE");
    assertThat(result.getName()).isEqualTo("Apple");
    assertThat(result.getUnitPrice()).isEqualTo("0.30");
    assertThat(result.getOffer().getQuantity()).isEqualTo(2);
    assertThat(result.getOffer().getPrice()).isEqualTo("0.45");
  }

  @Test
  void mapsAbsentOfferAndEnvelope() {
    var item = mapper.toRest(product, null);
    var result = mapper.toCatalog(List.of(item), "test-revision");

    assertThat(item.getOffer()).isNull();
    assertThat(result.getItems()).containsExactly(item);
    assertThat(result.getTotalItems()).isEqualTo(1);
    assertThat(result.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(mapper.toCatalog(List.of(), "test-revision").getTotalItems()).isZero();
  }
}
