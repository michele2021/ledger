package com.accounting.core.entities.views;

import com.accounting.commons.Constants;
import com.accounting.commons.Constants.JournalType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalView {

    private long journalId;
    @JsonIgnore
    private OffsetDateTime bookingDateAsOffset;
    private JournalType journalTypeCode;

    public String getBookingDate() {
        return bookingDateAsOffset == null ? null : bookingDateAsOffset.format(Constants.dateTimeFormatter);
    }

}
