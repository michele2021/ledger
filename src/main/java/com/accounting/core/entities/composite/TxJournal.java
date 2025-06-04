package com.accounting.core.entities.composite;

import com.accounting.core.entities.Tx;
import com.accounting.core.entities.views.JournalView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
// TODO currently not used
public class TxJournal extends Tx {

    private final List<JournalView> journals = new ArrayList<>();

    public void addJournal(JournalView jv) {
        journals.add(jv);
    }
}
