package com.accounting.core.entities;

import com.accounting.commons.Constants;
import lombok.Builder;

import java.util.List;

@Builder
public record Order(
        Constants.Platform platform,
        long orderNumber,
        String totalTax,
        String billingAddressCountry,
        String shippingAddressCountry,
        String createdAt,
        List<OrderItem> orderItems,
        List<TaxLine> taxLines) {

    public OrderDTO orderDTO(long txId, long platformId) {
        return new OrderDTO(platformId, txId, orderNumber, totalTax, billingAddressCountry, shippingAddressCountry, createdAt, orderItems, taxLines);
    }
}
