package com.accounting.core.processing.templates;

import com.accounting.commons.entities.JournalType;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.processing.JournalMaker;
import lombok.Getter;

import java.util.List;

@Getter
public class JournalTemplateIf4TestNoCtor implements JournalTemplateIf {

    public JournalType journalType;

    public JournalMaker journalMaker;

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        return null;
    }

    @Override
    public boolean isTestAccount() {
        return false;
    }
}