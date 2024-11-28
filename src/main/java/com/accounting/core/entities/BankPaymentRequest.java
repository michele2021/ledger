package com.accounting.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankPaymentRequest {

    private String requestId;
    private String provider;
    private long accountIdFrom;
    private String accountCodeFrom;
    private long accountIdTo;
    private String accountCodeTo;
    private String status;
}
