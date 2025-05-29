package com.accounting.core.entities;

import lombok.Builder;

@Builder
public record PaymentDTO(long paymentProviderId,
                         long txId,
                         String paymentMethod) implements SpecializedTx {
}