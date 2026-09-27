package com.example.supermarket.checkout.api;

import com.example.supermarket.checkout.application.CheckoutCommand;
import com.example.supermarket.checkout.domain.AppliedOffer;
import com.example.supermarket.checkout.domain.CartItem;
import com.example.supermarket.checkout.domain.Receipt;
import com.example.supermarket.checkout.domain.ReceiptLine;
import com.example.supermarket.generated.model.CheckoutItem;
import com.example.supermarket.generated.model.CheckoutReceipt;
import com.example.supermarket.generated.model.CheckoutRequest;
import com.example.supermarket.support.api.RestValueMapper;
import java.util.Optional;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = RestValueMapper.class)
public interface CheckoutRestMapper {
  CheckoutCommand toCommand(CheckoutRequest request);

  CartItem toCartItem(CheckoutItem item);

  @Mapping(target = "currency", constant = "EUR")
  CheckoutReceipt toRest(Receipt receipt, String catalogRevision);

  com.example.supermarket.generated.model.ReceiptLine toRest(ReceiptLine line);

  @Mapping(target = "price", source = "bundlePrice")
  com.example.supermarket.generated.model.AppliedOffer toRest(AppliedOffer offer);

  default com.example.supermarket.generated.model.AppliedOffer toRest(
      Optional<AppliedOffer> offer) {
    return offer.map(this::toRest).orElse(null);
  }
}
