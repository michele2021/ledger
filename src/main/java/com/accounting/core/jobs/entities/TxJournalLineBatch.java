package com.accounting.core.jobs.entities;

import com.accounting.core.entities.JournalLine;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TxJournalLineBatch extends JournalLine {

    private long txId;
    private long journalLineId;
    private int accountId;
    private int registerTypeId;
    private String accountCode;
    private String accountTypeCode;

    @Override
    public String toString() {
        return "TxJournalLineBatch{" +
                "txId=" + txId +
                ", journalLineId=" + journalLineId +
                ", registerTypeId=" + registerTypeId +
                ", batch accountCode='" + accountCode + '\'' +
                ", accountTypeCode='" + accountTypeCode + '\'' +
                "}\n";
    }
}
