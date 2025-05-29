package com.accounting.core.entities;

import com.accounting.commons.Constants;
import com.accounting.core.db.JournalDAO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CCToken implements SpecializedTx {

    private long txId;
    private Long orderId;
    private String publishableId; // token serial number (used by Epay)
    private LocalDate expiryDate; // No timezone, it is just a date, no time.
    private String locationRef; // used when the token is created in inactive state
    private String activatingLocationRef;
    private OffsetDateTime activationTimestamp;
    private String activationTimezone;
    private Constants.TokenType type;
    private String orderItemId;
    private String prepaidCardExternalId;
    private String clientMetadata;
    private String client; // for AG and other distributors, this is the client of AG that ordered the card.

    @Override
    public void save(JournalDAO dao) {
        //
    }
}
