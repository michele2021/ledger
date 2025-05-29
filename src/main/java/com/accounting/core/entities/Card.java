package com.accounting.core.entities;

import com.accounting.core.db.JournalDAO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card implements SpecializedTx {

    private long txId;
    private String prepaidCardExternalId;
    private LocalDate expiryDate;

    @Override
    public void save(JournalDAO dao) {
        //
    }
}