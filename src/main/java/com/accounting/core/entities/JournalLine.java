package com.accounting.core.entities;

import com.accounting.commons.entities.Amount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLine {

    private long batchId;
    private long quantity;
    private long journalId;
    private int currencyId;

    public JournalLine(JournalLineBuilder builder) {
        this.batchId = builder.batchId;
        this.quantity = builder.quantity;
        this.journalId = builder.journalId;
        this.currencyId = builder.currencyId;
    }

    public static class JournalLineBuilder {
        private long quantity;
        private int currencyId;

        public JournalLineBuilder debit(Amount amount) {
            this.currencyId = amount.getCurrencyId();
            this.quantity = Math.abs(amount.getQuantity());
            return this;
        }

        public JournalLineBuilder credit(Amount amount) {
            this.currencyId = amount.getCurrencyId();
            this.quantity = -Math.abs(amount.getQuantity());
            return this;
        }
    }

    public boolean isDebit() {
        return quantity >= 0;
    }
}
