package com.accounting.core.entities;

import com.accounting.commons.Constants;

public record Payment(String paymentMethod, Constants.PaymentProvider paymentProvider) {

    public PaymentDTO paymentDTO(long txId, long paymentProviderId) {
        return new PaymentDTO(paymentProviderId, txId, paymentMethod);
    }
}