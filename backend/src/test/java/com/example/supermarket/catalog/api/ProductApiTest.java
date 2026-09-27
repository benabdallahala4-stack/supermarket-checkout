package com.example.supermarket.catalog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.ActiveProfiles("config-catalog")
@SpringBootTest
@AutoConfigureMockMvc
class ProductApiTest {
  @Autowired private MockMvc mvc;

  @Test
  void listsSortedProductsAndOffersAsExactMoneyStrings() throws Exception {
    mvc.perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
            {"currency":"EUR","totalItems":3,"items":[
              {"id":"APPLE","name":"Apple","unitPrice":"0.30","offer":{"quantity":2,"price":"0.45"}},
              {"id":"BANANA","name":"Banana","unitPrice":"0.50","offer":{"quantity":3,"price":"1.20"}},
              {"id":"ORANGE","name":"Orange","unitPrice":"0.80"}]}
            """,
                    JsonCompareMode.STRICT))
        .andExpect(jsonPath("$.items[2].offer").doesNotHaveJsonPath());
  }

  @Test
  void doesNotDuplicateApiBasePath() throws Exception {
    mvc.perform(get("/api/api/products")).andExpect(status().isNotFound());
  }
}
