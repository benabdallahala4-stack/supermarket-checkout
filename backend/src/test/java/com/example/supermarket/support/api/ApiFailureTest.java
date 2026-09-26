package com.example.supermarket.support.api;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.CatalogSnapshot;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFailureTest {
  @Autowired private MockMvc mvc;
  @MockitoBean private CatalogProvider provider;

  static Stream<Arguments> failures() {
    return Stream.of(
        Arguments.of("/api/products", new IllegalStateException("private diagnostic")),
        Arguments.of("/api/checkout", new IllegalStateException("private diagnostic")),
        Arguments.of("/api/products", new IllegalArgumentException("private diagnostic")),
        Arguments.of("/api/checkout", new IllegalArgumentException("private diagnostic")));
  }

  @ParameterizedTest
  @MethodSource("failures")
  void returnsSafeServerErrors(String path, RuntimeException failure) throws Exception {
    when(provider.findAll()).thenThrow(failure);
    when(provider.findFor(anySet())).thenThrow(failure);
    var request =
        path.endsWith("products")
            ? get(path)
            : post(path).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}");

    mvc.perform(request)
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.title").value("Internal server error"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").value("The request could not be completed."))
        .andExpect(jsonPath("$.instance").value(path))
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.errors").doesNotHaveJsonPath())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("private diagnostic"))));
  }

  @Test
  void returnsEmptyCatalogEnvelope() throws Exception {
    when(provider.findAll()).thenReturn(new CatalogSnapshot(List.of(), List.of()));

    mvc.perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("EUR"))
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.totalItems").value(0));
  }
}
