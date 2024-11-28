package com.accounting.core.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BankPaymentRequestIban extends BankPaymentRequest {
    private String ibanFrom;
    private long linkedAccountIdFrom;
    private String linkedAccountCodeFrom;
    private String ibanTo;
}
