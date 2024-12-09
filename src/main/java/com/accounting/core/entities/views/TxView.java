package com.accounting.core.entities.views;

import com.accounting.commons.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxView {

    private long txId;
    @JsonIgnore
    private Long quantityAsLong;
    private int currencyId;
    private String txReference;
    private long accountId;
    private String accountCode;
    private Long parentTxId;
    private Constants.TxType txTypeCode;
    @JsonIgnore
    private OffsetDateTime creationDateAsOffset;
    private Constants.Currencies currencyCode;

    public BigDecimal getQuantity() {
        return Constants.fromMU(currencyCode, quantityAsLong);
    }

    public String getCreationDate() {
        return creationDateAsOffset == null ? null : creationDateAsOffset.format(Constants.dateTimeFormatter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TxView txView = (TxView) o;
        return txId == txView.txId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(txId);
    }

}
