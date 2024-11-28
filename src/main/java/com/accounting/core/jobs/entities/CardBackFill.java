package com.accounting.core.jobs.entities;

import com.accounting.commons.entities.Amount;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.Nested;

@Data
@NoArgsConstructor
public class CardBackFill {
    Long txId; // token tx id
    String prepaidCardExternalId;
    Amount amount;

    String accountCode;
    String accountTypeCode;

    @Nested
    public void setAmount(Amount amount) {
        this.amount = amount;
    }
}