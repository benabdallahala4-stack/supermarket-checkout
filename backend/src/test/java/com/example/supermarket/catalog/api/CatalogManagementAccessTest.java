package com.example.supermarket.catalog.api;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CatalogManagementAccessTest {
  private ApplicationContextRunner context(String... profiles) {
    return new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().setActiveProfiles(profiles))
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withUserConfiguration(CatalogManagementAccess.class);
  }

  @Test
  void enabledManagementRequiresExplicitStrongToken() {
    for (String token : new String[] {"", "short", "01234567890123456789012345678901 x"}) {
      context("catalog-management")
          .withPropertyValues("checkout.catalog-management.token=" + token)
          .run(context -> assertThat(context).hasFailed());
    }
  }

  @Test
  void cannotEnableManagementForYamlCatalog() {
    context("catalog-management", "config-catalog")
        .withPropertyValues(
            "checkout.catalog-management.token=012345678901234567890123456789012345")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void disabledManagementDoesNotRequireAnySecret() {
    context("config-catalog")
        .run(
            context ->
                assertThat(context).hasNotFailed().doesNotHaveBean(CatalogManagementAccess.class));
  }
}
