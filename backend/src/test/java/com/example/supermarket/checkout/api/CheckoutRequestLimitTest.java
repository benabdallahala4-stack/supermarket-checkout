package com.example.supermarket.checkout.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("config-catalog")
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutRequestLimitTest {
  private static final String PATH = "/api/checkout";

  @Autowired MockMvc mvc;
  @MockitoBean CatalogProvider provider;

  @Test
  void rejectsMoreThanOneThousandCartLinesBeforeCatalogLookup() throws Exception {
    var items =
        IntStream.rangeClosed(1, 1_001)
            .mapToObj(index -> "{\"productId\":\"APPLE\",\"quantity\":1}")
            .collect(Collectors.joining(","));

    mvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[" + items + "]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    verifyNoInteractions(provider);
  }

  @Test
  void rejectsOversizedJsonBeforeCatalogLookup() throws Exception {
    var body = " ".repeat(1_048_577) + "{\"items\":[]}";

    mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    verifyNoInteractions(provider);
  }
}
