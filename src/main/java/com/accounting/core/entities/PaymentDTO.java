package com.accounting.core.entities;

import com.accounting.core.db.JournalDAO;
import lombok.Builder;

@Builder
public record PaymentDTO(long paymentProviderId,
                         long txId,
                         String paymentMethod) implements SpecializedTx {

    @Override
    public void save(JournalDAO dao) {
        dao.createPayment(this);
    }
}