package com.example.supermarket.catalog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("config-catalog")
class CatalogRevisionApiTest {
  @Autowired MockMvc mvc;
  @Autowired CatalogProvider provider;

  @Test
  void publicCatalogAndReceiptsCarryTheActualReadRevisionIncludingEmptyCheckout() throws Exception {
    String revision = provider.findAll().revision();
    mvc.perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.catalogRevision").value(revision))
        .andExpect(header().string("Cache-Control", "no-store"));
    for (String items : new String[] {"[]", "[{\"productId\":\"APPLE\",\"quantity\":3}]"}) {
      mvc.perform(
              post("/api/checkout")
                  .contentType("application/json")
                  .content("{\"items\":" + items + "}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.catalogRevision").value(revision));
    }
  }
}
