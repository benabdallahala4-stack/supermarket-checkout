package com.example.supermarket.catalog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("config-catalog")
class CatalogManagementDisabledTest {
  @Autowired MockMvc mvc;

  @Test
  void endpointDoesNotExistWithoutManagementProfile() throws Exception {
    mvc.perform(get("/api/management/catalog")).andExpect(status().isNotFound());
    mvc.perform(put("/api/management/catalog").contentType("application/json").content("{}"))
        .andExpect(status().isNotFound());
  }
}
