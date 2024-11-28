package com.accounting.core.jobs.entities;

import com.accounting.core.entities.views.TxView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentView extends TxView {

    String bankAccountCode;
    BigDecimal amount;
    BigDecimal discount;
    BigDecimal fee;
    BigDecimal vat;
}