package com.accounting.core.entities;

import com.accounting.core.db.JournalDAO;
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
    public void save(JournalDAO dao) {
        dao.createOrder(this);
    }
}