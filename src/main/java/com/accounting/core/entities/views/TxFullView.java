package com.accounting.core.entities.views;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TxFullView extends TxView {

    private final List<JournalView> journals = new ArrayList<>();

    public void addJournal(JournalView jv) {
        journals.add(jv);
    }

}
