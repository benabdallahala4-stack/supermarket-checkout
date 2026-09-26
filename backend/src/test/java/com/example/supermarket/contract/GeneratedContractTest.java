package com.example.supermarket.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.supermarket.generated.model.CheckoutItem;
import com.example.supermarket.generated.model.CheckoutRequest;
import com.example.supermarket.generated.model.Product;
import com.example.supermarket.generated.model.ReceiptLine;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class GeneratedContractTest {

  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private static final Validator VALIDATOR = FACTORY.getValidator();
  private final ObjectMapper mapper = new ObjectMapper();

  @AfterAll
  static void closeValidatorFactory() {
    FACTORY.close();
  }

  @Test
  void acceptsARealProductIdentifierAndWholeQuantity() {
    assertThat(VALIDATOR.validate(new CheckoutItem().productId("APPLE").quantity(3))).isEmpty();
  }

  @Test
  void rejectsBlankIdentifiersAndZeroQuantities() {
    assertThat(VALIDATOR.validate(new CheckoutItem().productId(" ").quantity(0)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("productId", "quantity");
  }

  @Test
  void distinguishesMissingItemsFromAnEmptyCart() {
    assertThat(VALIDATOR.validate(new CheckoutRequest()))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("items");
    assertThat(VALIDATOR.validate(new CheckoutRequest().items(java.util.List.of()))).isEmpty();
  }

  @Test
  void serializesMoneyAsExactStringsAndOmitsUnusedOffers() throws Exception {
    var product =
        mapper.readTree(
            mapper.writeValueAsString(new Product().id("ORANGE").name("Orange").unitPrice("0.80")));
    assertThat(product.get("unitPrice").isTextual()).isTrue();
    assertThat(product.get("unitPrice").textValue()).isEqualTo("0.80");
    assertThat(product.has("offer")).isFalse();

    var line =
        mapper.readTree(
            mapper.writeValueAsString(
                new ReceiptLine()
                    .productId("ORANGE")
                    .name("Orange")
                    .quantity(1)
                    .unitPrice("0.80")
                    .subtotal("0.80")
                    .discount("0.00")
                    .total("0.80")));
    assertThat(line.has("appliedOffer")).isFalse();
    assertThat(line.get("total").textValue()).isEqualTo("0.80");
  }

  @Test
  void rejectsFractionalCentPricesAtTheContractBoundary() {
    assertThat(VALIDATOR.validate(new Product().id("APPLE").name("Apple").unitPrice("0.301")))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("unitPrice");
  }
}
