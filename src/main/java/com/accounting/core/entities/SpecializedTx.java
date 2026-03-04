package com.accounting.core.entities;

import com.accounting.core.db.JournalDAO;

public sealed interface SpecializedTx permits PaymentDTO, OrderDTO {
    void save(JournalDAO dao);
}