package com.example.supermarket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@org.springframework.test.context.ActiveProfiles("config-catalog")
@SpringBootTest
class SupermarketApplicationTest {

  @Autowired private ApplicationContext context;

  @Test
  void startsTheApplicationContext() {
    assertThat(context).isNotNull();
    assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
  }
}
