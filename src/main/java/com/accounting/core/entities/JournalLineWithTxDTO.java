package com.accounting.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLineWithTxDTO {

    private long txId;
    private long batchId;
    private long journalLineId;
    private long journalId;
    private int currencyId;
    private long quantity;
    private int registerTypeId;
    private String registerTypeCode;
    private long accountId;

    public boolean isDebit() {
        return quantity >= 0;
    }
}
