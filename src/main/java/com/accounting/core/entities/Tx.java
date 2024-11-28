package com.accounting.core.entities;

import com.accounting.commons.entities.Amount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.Nested;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tx {

    private long txId;
    private int txTypeId;
    private long accountId;
    private Long parentTxId;
    private int txVariantId;
    private String txReference;
    private Amount amount;
    private OffsetDateTime creationDate;
    private String description;

    @Nested
    public void setAmount(Amount amount) {
        this.amount = amount;
    }

}
