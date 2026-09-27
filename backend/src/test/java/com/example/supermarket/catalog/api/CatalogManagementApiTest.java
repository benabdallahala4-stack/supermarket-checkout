package com.example.supermarket.catalog.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("database")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("catalog-management")
class CatalogManagementApiTest {
  private static final String TOKEN = "test-only-catalog-token-0123456789abcdef";
  private static final String PATH = "/api/management/catalog";
  @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16.15");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", DB::getJdbcUrl);
    registry.add("spring.datasource.username", DB::getUsername);
    registry.add("spring.datasource.password", DB::getPassword);
    registry.add("checkout.catalog-management.token", () -> TOKEN);
  }

  @org.springframework.boot.test.web.server.LocalServerPort int port;
  @Autowired MockMvc mvc;
  @Autowired CatalogProvider provider;

  @Test
  void authorizedReplacementChangesPublicCatalogAndCheckoutWithoutRestart() throws Exception {
    var before = provider.findAll();
    mvc.perform(get(PATH).header("X-Catalog-Token", TOKEN))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", etag(before.revision())))
        .andExpect(jsonPath("$.revision").value(before.revision()));
    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .header("If-Match", etag(before.revision()))
                .contentType("application/json")
                .content(
                    replacement(
                        "[{\"id\":\"TEA\",\"name\":\"Tea\",\"unitPrice\":\"2.00\",\"offer\":{\"quantity\":3,\"price\":\"5.00\"}}]")))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(jsonPath("$.items[0].id").value("TEA"));
    assertThat(provider.findAll().revision()).isNotEqualTo(before.revision());
    mvc.perform(get("/api/products"))
        .andExpect(jsonPath("$.catalogRevision").value(provider.findAll().revision()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("TEA"));
    mvc.perform(
            post("/api/checkout")
                .contentType("application/json")
                .content("{\"items\":[{\"productId\":\"TEA\",\"quantity\":4}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value("7.00"))
        .andExpect(jsonPath("$.catalogRevision").value(provider.findAll().revision()));
    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .header("If-Match", etag(before.revision()))
                .contentType("application/json")
                .content(replacement("[]")))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("CATALOG_CONFLICT"));
    assertThat(provider.findAll().products()).hasSize(1);
  }

  @Test
  void rejectsMissingAndWrongCredentialsBeforeReadingBody() throws Exception {
    var before = provider.findAll();
    for (String token : new String[] {"", "wrong"}) {
      mvc.perform(get(PATH).header("X-Catalog-Token", token)).andExpect(status().isUnauthorized());
      mvc.perform(
              put(PATH)
                  .header("X-Catalog-Token", token)
                  .contentType("application/json")
                  .content("malformed"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
    assertThat(provider.findAll().revision()).isEqualTo(before.revision());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "null",
        "[null]",
        "[{\"id\":\"X\",\"name\":\" \",\"unitPrice\":\"1.00\"}]",
        "[{\"id\":\"X\",\"name\":\"X\",\"unitPrice\":\"1.001\"}]",
        "[{\"id\":\"X\",\"name\":\"X\",\"unitPrice\":1.00}]",
        "[{\"id\":\"X\",\"name\":\"X\",\"unitPrice\":\"1.00\",\"offer\":{\"quantity\":2,\"price\":\"2.00\"}}]",
        "[{\"id\":\"X\",\"name\":\"X\",\"unitPrice\":\"1.00\"},{\"id\":\"X\",\"name\":\"X\",\"unitPrice\":\"1.00\"}]"
      })
  void rejectsInvalidCatalogWithoutChangingData(String items) throws Exception {
    var before = provider.findAll();
    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .header("If-Match", etag(before.revision()))
                .contentType("application/json")
                .content(replacement(items)))
        .andExpect(status().isBadRequest());
    assertThat(provider.findAll().revision()).isEqualTo(before.revision());
    assertThat(provider.findAll().products()).isEqualTo(before.products());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "[{\"id\":\"X\",\"name\":\"X\",\"unitPrice\":\"1.00\",\"extra\":true}]",
        "[{\"id\":\"X\",\"name\":\"X\",\"unitPrice\":\"1.00\",\"offer\":{\"quantity\":2,\"price\":\"1.00\",\"extra\":true}}]",
        "[{\"id\":\"X\",\"name\":\"   \",\"unitPrice\":\"1.00\"}]"
      })
  void rejectsContractInvalidCatalogFieldsWithGenericProblem(String items) throws Exception {
    var before = provider.findAll();

    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .header("If-Match", etag(before.revision()))
                .contentType("application/json")
                .content(replacement(items)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.detail").value("The request body contains invalid data or JSON."));

    assertThat(provider.findAll().revision()).isEqualTo(before.revision());
    assertThat(provider.findAll().products()).isEqualTo(before.products());
  }

  @Test
  void acceptsIntentionalEmptyReplacement() throws Exception {
    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .header("If-Match", etag(provider.findAll().revision()))
                .contentType("application/json")
                .content(replacement("[]")))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(jsonPath("$.items").isEmpty());
    mvc.perform(get("/api/products"))
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.catalogRevision").value(provider.findAll().revision()));
  }

  private String replacement(String items) {
    return "{\"items\":" + items + "}";
  }

  private String etag(String revision) {
    return "\"" + revision + "\"";
  }

  @Test
  void actualNetworkRequestsRequireHeaderAndAreNeverCached() throws Exception {
    try (var client = java.net.http.HttpClient.newHttpClient()) {
      var uri = java.net.URI.create("http://127.0.0.1:" + port + "/api/management/catalog");
      var denied =
          client.send(
              java.net.http.HttpRequest.newBuilder(uri).GET().build(),
              java.net.http.HttpResponse.BodyHandlers.ofString());
      assertThat(denied.statusCode()).isEqualTo(401);
      var allowed =
          client.send(
              java.net.http.HttpRequest.newBuilder(uri)
                  .header("X-Catalog-Token", TOKEN)
                  .GET()
                  .build(),
              java.net.http.HttpResponse.BodyHandlers.ofString());
      assertThat(allowed.statusCode()).isEqualTo(200);
      assertThat(allowed.headers().firstValue("Cache-Control")).contains("no-store");
      assertThat(allowed.headers().firstValue("ETag"))
          .contains(etag(provider.findAll().revision()));
      assertThat(allowed.body()).doesNotContain(TOKEN);
    }
  }

  @Test
  void requiresIfMatchWithoutChangingData() throws Exception {
    var before = provider.findAll();

    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .contentType("application/json")
                .content(replacement("[]")))
        .andExpect(status().isPreconditionRequired())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    assertThat(provider.findAll().revision()).isEqualTo(before.revision());
  }

  @ParameterizedTest
  @ValueSource(strings = {"bad", "*", "W/\"00000000-0000-0000-0000-000000000000\""})
  void rejectsMalformedIfMatchWithoutChangingData(String ifMatch) throws Exception {
    var before = provider.findAll();

    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .header("If-Match", ifMatch)
                .contentType("application/json")
                .content(replacement("[]")))
        .andExpect(status().isBadRequest());

    assertThat(provider.findAll().revision()).isEqualTo(before.revision());
  }

  @Test
  void rejectsMoreThanOneThousandProductsWithoutChangingData() throws Exception {
    var before = provider.findAll();

    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .header("If-Match", etag(before.revision()))
                .contentType("application/json")
                .content(replacement("[" + realisticProducts(1_001) + "]")))
        .andExpect(status().isBadRequest());

    assertThat(provider.findAll().revision()).isEqualTo(before.revision());
    assertThat(provider.findAll().products()).isEqualTo(before.products());
  }

  @Test
  void acceptsOneThousandRealisticProductsWithOffers() throws Exception {
    var before = provider.findAll();

    mvc.perform(
            put(PATH)
                .header("X-Catalog-Token", TOKEN)
                .header("If-Match", etag(before.revision()))
                .contentType("application/json")
                .content(replacement("[" + realisticProducts(1_000) + "]")))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(jsonPath("$.items.length()").value(1_000));
  }

  private String realisticProducts(int count) {
    return IntStream.range(0, count)
        .mapToObj(
            index ->
                "{\"id\":\"PRODUCT%04d\",\"name\":\"Representative supermarket product number %04d\",\"unitPrice\":\"1.00\",\"offer\":{\"quantity\":2,\"price\":\"1.50\"}}"
                    .formatted(index, index))
        .collect(Collectors.joining(","));
  }
}
