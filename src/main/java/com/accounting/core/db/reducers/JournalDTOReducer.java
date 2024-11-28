package com.accounting.core.db.reducers;

import com.accounting.core.entities.JournalDTO;
import com.accounting.core.entities.JournalLineDTO;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;

public class JournalDTOReducer implements LinkedHashMapRowReducer<Long, JournalDTO> {

    @Override
    public void accumulate(Map<Long, JournalDTO> map, RowView rowView) {
        JournalDTO j = map.computeIfAbsent(rowView.getColumn("j_journalId", Long.class),
                id -> rowView.getRow(JournalDTO.class));

        if (rowView.getColumn("jl_journalLineId", Long.class) != null) {
            j.getJournalLines().add(rowView.getRow(JournalLineDTO.class));
        }
    }
}
