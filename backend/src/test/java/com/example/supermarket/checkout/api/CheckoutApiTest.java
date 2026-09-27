package com.example.supermarket.checkout.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.ActiveProfiles("config-catalog")
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {
  @Autowired private com.example.supermarket.catalog.application.port.CatalogProvider provider;
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;

  private static String item(String id, String quantity) {
    return "{\"productId\":" + id + ",\"quantity\":" + quantity + "}";
  }

  private static String cart(String items) {
    return "{\"items\":[" + items + "]}";
  }

  @Test
  void returnsExactThreeAppleReceipt() throws Exception {
    mvc.perform(
            post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cart(item("\"APPLE\"", "3"))))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
            {"catalogRevision":"%s","currency":"EUR","items":[{"productId":"APPLE","name":"Apple","quantity":3,
            "unitPrice":"0.30","subtotal":"0.90","discount":"0.15","total":"0.75",
            "appliedOffer":{"quantity":2,"price":"0.45","applications":1}}],
            "subtotal":"0.90","discount":"0.15","total":"0.75"}
            """
                        .formatted(provider.findAll().revision()),
                    JsonCompareMode.STRICT));
  }

  @Test
  void emptyCheckoutReturnsExactZeroReceipt() throws Exception {
    mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(cart("")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
            {"catalogRevision":"%s","currency":"EUR","items":[],"subtotal":"0.00","discount":"0.00","total":"0.00"}
            """
                        .formatted(provider.findAll().revision()),
                    JsonCompareMode.STRICT));
  }

  @Test
  void aggregatesDuplicatesAndSortsLines() throws Exception {
    mvc.perform(
            post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cart(
                        item("\"BANANA\"", "4")
                            + ","
                            + item("\"APPLE\"", "1")
                            + ","
                            + item("\"APPLE\"", "2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].productId").value("APPLE"))
        .andExpect(jsonPath("$.items[0].quantity").value(3))
        .andExpect(jsonPath("$.items[1].productId").value("BANANA"))
        .andExpect(jsonPath("$.total").value("2.45"));
  }

  @Test
  void omitsUnappliedOffers() throws Exception {
    mvc.perform(
            post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cart(item("\"APPLE\"", "1") + "," + item("\"ORANGE\"", "1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value("1.10"))
        .andExpect(jsonPath("$.items[0].appliedOffer").doesNotHaveJsonPath())
        .andExpect(jsonPath("$.items[1].appliedOffer").doesNotHaveJsonPath());
  }

  static Stream<String> invalidBodies() {
    return Stream.of(
        "",
        "{",
        "null",
        "[]",
        "{}",
        "{\"items\":null}",
        cart("null"),
        "{\"items\":{},\"other\":1}",
        "{\"items\":[],\"extra\":true}",
        cart("{\"productId\":\"APPLE\",\"quantity\":1,\"price\":\"0.01\"}"),
        cart("{\"productId\":\"APPLE\"}"),
        cart("{\"quantity\":1}"),
        cart(item("\"\"", "1")),
        cart(item("\"   \"", "1")),
        cart(item("null", "1")),
        cart(item("123", "1")),
        cart(item("true", "1")),
        cart(item("\"\u2003\"", "1")),
        cart(item("\"APPLE\"", "0")),
        cart(item("\"APPLE\"", "-1")),
        cart(item("\"APPLE\"", "1.0")),
        cart(item("\"APPLE\"", "1e0")),
        cart(item("\"APPLE\"", "\"3\"")),
        cart(item("\"APPLE\"", "\"\"")),
        cart(item("\"APPLE\"", "true")),
        cart(item("\"APPLE\"", "null")),
        cart(item("\"APPLE\"", "2147483648")),
        cart("") + " {}");
  }

  @ParameterizedTest
  @MethodSource("invalidBodies")
  void rejectsMalformedOrInvalidRequests(String body) throws Exception {
    mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.detail").value("The request body contains invalid data or JSON."))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.instance").value("/api/checkout"))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void rejectsUnknownProductWithoutPartialReceipt() throws Exception {
    mvc.perform(
            post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cart(item("\"APPLE\"", "1") + "," + item("\"apple\"", "1"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNKNOWN_PRODUCT"))
        .andExpect(jsonPath("$.items").doesNotHaveJsonPath());
  }

  @Test
  void rejectsAggregateOverflow() throws Exception {
    mvc.perform(
            post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cart(item("\"APPLE\"", "2147483647") + "," + item("\"APPLE\"", "1"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void acceptsLargestValidQuantity() throws Exception {
    mvc.perform(
            post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cart(item("\"APPLE\"", "2147483647"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value("483183820.65"));
  }

  @Test
  void rejectsUnsupportedMediaType() throws Exception {
    mvc.perform(post("/api/checkout").contentType(MediaType.TEXT_PLAIN).content(cart("")))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.instance").value("/api/checkout"));
  }

  @Test
  void reportsFieldErrorsInDeterministicOrder() throws Exception {
    var request =
        post("/api/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cart(item("\"\"", "0")));
    String body =
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var errors = objectMapper.readTree(body).get("errors");
    assertThat(errors).isNotNull();
    var keys = new ArrayList<String>();
    errors.forEach(
        error -> keys.add(error.get("field").asText() + ":" + error.get("message").asText()));

    assertThat(keys).isNotEmpty().isSorted();
    assertThat(keys).anyMatch(key -> key.startsWith("items[0].quantity:"));
    assertThat(keys).anyMatch(key -> key.startsWith("items[0].productId:"));
    mvc.perform(request).andExpect(content().json(body, JsonCompareMode.STRICT));
  }
}
