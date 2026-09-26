package com.example.supermarket.checkout.configuration;

import com.example.supermarket.checkout.domain.PricingCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CheckoutConfiguration {
  @Bean
  PricingCalculator pricingCalculator() {
    return new PricingCalculator();
  }
}
