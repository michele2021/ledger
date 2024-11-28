package com.accounting.core.reports.model;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Currency;
import com.accounting.core.reports.enums.ProductType;
import com.accounting.core.reports.enums.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesReportRow {

    private Long rowNumber;
    private ProductType productType;
    private TransactionType transactionType;
    private String EAN;
    private String serialNumber;
    private String terminalId;
    private String externalId;
    private LocalDateTime bookingDate;
    private Long grossAmountQuantity;
    private Long netAmountQuantity;
    private Long commissionAmountQuantity;
    private Currency currency;
    private String txReference;
    private String activatingLocationRef;

    public Constants.JournalType getJournalType() {
        return transactionType.getJournalType();
    }

    public int getCurrencyId() {
        return currency.getCurrencyId();
    }

    public String getCurrencyCode() {
        return currency.getCode();
    }
}
