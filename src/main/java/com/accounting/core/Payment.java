package com.accounting.core;

import com.accounting.commons.Constants;
import com.accounting.core.entities.PaymentDTO;

public record Payment(String paymentMethod, Constants.PaymentProvider paymentProvider) {

    public PaymentDTO paymentDTO(long txId, long paymentProviderId) {
        return new PaymentDTO(paymentProviderId, txId, paymentMethod);
    }
}