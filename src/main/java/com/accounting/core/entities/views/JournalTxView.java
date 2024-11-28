package com.accounting.core.entities.views;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JournalTxView {

    private long journalId;
    private int currencyId;
    private int accountId;
    private long quantity;

}
