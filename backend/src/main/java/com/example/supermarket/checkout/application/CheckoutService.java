package com.example.supermarket.checkout.application;

import com.example.supermarket.catalog.application.port.CatalogProvider;
import com.example.supermarket.catalog.domain.ProductId;
import com.example.supermarket.checkout.domain.CartItem;
import com.example.supermarket.checkout.domain.InvalidCartException;
import com.example.supermarket.checkout.domain.PricingCalculator;
import com.example.supermarket.checkout.domain.Receipt;
import com.example.supermarket.checkout.domain.UnknownProductException;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
  private final CatalogProvider provider;
  private final PricingCalculator calculator;

  public CheckoutService(CatalogProvider provider, PricingCalculator calculator) {
    this.provider = provider;
    this.calculator = calculator;
  }

  public Receipt checkout(CheckoutCommand command) {
    if (command == null) {
      throw new InvalidCartException("Checkout command is required");
    }

    var ids = command.items().stream().map(CartItem::productId).collect(Collectors.toSet());
    var catalog = provider.findFor(ids);
    var orderedIds = ids.stream().sorted(Comparator.comparing(ProductId::value)).toList();

    for (ProductId id : orderedIds) {
      if (!catalog.products().containsKey(id)) {
        throw new UnknownProductException(id);
      }
    }

    return calculator.calculate(command.items(), catalog);
  }
}
