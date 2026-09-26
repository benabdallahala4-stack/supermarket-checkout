package com.example.supermarket.checkout.api;

import com.example.supermarket.checkout.application.CheckoutCommand;
import com.example.supermarket.checkout.application.CheckoutService;
import com.example.supermarket.checkout.domain.InvalidCartException;
import com.example.supermarket.generated.api.CheckoutApi;
import com.example.supermarket.generated.model.CheckoutReceipt;
import com.example.supermarket.generated.model.CheckoutRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController implements CheckoutApi {
  private final CheckoutService service;
  private final CheckoutRestMapper mapper;

  public CheckoutController(CheckoutService service, CheckoutRestMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  public ResponseEntity<CheckoutReceipt> calculateCheckout(CheckoutRequest request) {
    CheckoutCommand command = toCommand(request);
    var receipt = service.checkout(command);

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(mapper.toRest(receipt));
  }

  private CheckoutCommand toCommand(CheckoutRequest request) {
    try {
      return mapper.toCommand(request);
    } catch (IllegalArgumentException exception) {
      // Translate invalid input values at the boundary, not failures from the use case.
      throw new InvalidCartException("Cart contains invalid items");
    }
  }
}
