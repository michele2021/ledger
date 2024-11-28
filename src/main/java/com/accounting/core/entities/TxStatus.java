package com.accounting.core.entities;

import com.accounting.commons.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TxStatus extends Tx {

    private long journalId;
    private Long issuerAccountId;
    private String journalTypeCode;
    private OffsetDateTime bookingDate;
    private Constants.TokenType tokenType;

}
