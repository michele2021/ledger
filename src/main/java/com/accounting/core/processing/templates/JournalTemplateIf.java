package com.accounting.core.processing.templates;

import com.accounting.commons.entities.JournalType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.SpecializedTx;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;

import java.time.OffsetDateTime;
import java.util.List;

public interface JournalTemplateIf {

    JournalType getJournalType();

    JournalMaker getJournalMaker();

    default OffsetDateTime getJournalBookingDate() {
        return null;
    }

    List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException;

    boolean isTestAccount();

    default boolean addNewTx() {
        return true;
    }

    default Long linkTx() {
        return null;
    }

    default Tx buildTx() {
        throw new UnsupportedOperationException("Operation buildTx not supported for the current template");
    }

    default List<SpecializedTx> buildSpecializedTxs(long txId) throws AccountingMakerException {
        throw new UnsupportedOperationException("Operation buildSpecializedTxs not supported for the current template");
    }

    default void updateSpecializedTx() throws AccountingMakerException {
        // nothing to do
    }

    default boolean addSpecializedTx() {
        return false;
    }

    default void process() throws AccountingMakerException {
        getJournalMaker().createJournal(this);
    }
}
