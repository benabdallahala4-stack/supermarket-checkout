package com.example.supermarket.checkout.api;

import static org.assertj.core.api.Assertions.*;

import com.example.supermarket.catalog.domain.*;
import com.example.supermarket.checkout.domain.*;
import com.example.supermarket.generated.model.CheckoutItem;
import com.example.supermarket.generated.model.CheckoutRequest;
import com.example.supermarket.generated.model.Currency;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles("config-catalog")
@SpringBootTest
class CheckoutRestMapperTest {
  @Autowired private CheckoutRestMapper mapper;

  @Test
  void mapsInputWithoutAggregatingDuplicates() {
    var request =
        new CheckoutRequest(
            new ArrayList<>(List.of(new CheckoutItem("APPLE", 1), new CheckoutItem("APPLE", 2))));
    var command = mapper.toCommand(request);

    assertThat(command.items())
        .containsExactly(
            new CartItem(new ProductId("APPLE"), 1), new CartItem(new ProductId("APPLE"), 2));
    request.getItems().clear();
    assertThat(command.items()).hasSize(2);
  }

  @Test
  void mapsCalculatedAmountsAndOfferToRestNames() {
    var id = new ProductId("APPLE");
    var catalog =
        new CatalogSnapshot(
            List.of(new Product(id, "Apple", new BigDecimal("0.30"))),
            List.of(new QuantityOffer(id, 2, new BigDecimal("0.45"))));
    var receipt = new PricingCalculator().calculate(List.of(new CartItem(id, 3)), catalog);
    var result = mapper.toRest(receipt);
    var line = result.getItems().getFirst();

    assertThat(result.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(result.getSubtotal()).isEqualTo("0.90");
    assertThat(result.getDiscount()).isEqualTo("0.15");
    assertThat(result.getTotal()).isEqualTo("0.75");
    assertThat(line.getProductId()).isEqualTo("APPLE");
    assertThat(line.getName()).isEqualTo("Apple");
    assertThat(line.getQuantity()).isEqualTo(3);
    assertThat(line.getUnitPrice()).isEqualTo("0.30");
    assertThat(line.getSubtotal()).isEqualTo("0.90");
    assertThat(line.getDiscount()).isEqualTo("0.15");
    assertThat(line.getTotal()).isEqualTo("0.75");
    assertThat(line.getAppliedOffer().getQuantity()).isEqualTo(2);
    assertThat(line.getAppliedOffer().getPrice()).isEqualTo("0.45");
    assertThat(line.getAppliedOffer().getApplications()).isEqualTo(1);

    var single =
        mapper.toRest(new PricingCalculator().calculate(List.of(new CartItem(id, 1)), catalog));
    assertThat(single.getItems().getFirst().getAppliedOffer()).isNull();
  }
}
