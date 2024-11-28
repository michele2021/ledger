package com.accounting.core.entities;


import com.accounting.commons.entities.Amount;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jdbi.v3.core.mapper.Nested;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class JournalLineDTO extends JournalLine {

    private long journalLineId;
    private int registerTypeId;
    private String registerTypeCode;
    private long accountId;
    private Amount amount;

    @Nested
    public void setAmount(Amount amount) {
        this.amount = amount;
    }

}
