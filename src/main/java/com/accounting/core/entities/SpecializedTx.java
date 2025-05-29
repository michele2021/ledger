package com.accounting.core.entities;

import com.accounting.core.db.JournalDAO;

public interface SpecializedTx {
    void save(JournalDAO dao);
}