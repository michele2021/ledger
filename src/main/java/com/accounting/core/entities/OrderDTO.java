package com.accounting.core.entities;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderDTO(long platformId,
                       long txId,
                       long orderNumber,
                       String totalTax,
                       String billingAddressCountry,
                       String shippingAddressCountry,
                       String createdAt,
                       List<OrderItem> orderItems,
                       List<TaxLine> taxLines) implements SpecializedTx {

    @Override
    public boolean isOrder() {
        return true;
    }
}