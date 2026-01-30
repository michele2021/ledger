package com.accounting.core.processing.templates;

import com.accounting.commons.entities.JournalType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.core.dto.JournalInfoDTO;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.SpecializedTx;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Core interface for journal template implementations.
 * Defines the contract for building journal entries, transactions, and specialized transactions.
 */
public interface JournalTemplateIf {

    /**
     * @return the journal type for this template
     */
    JournalType getJournalType();

    /**
     * @return the journal maker instance
     */
    JournalMaker getJournalMaker();

    /**
     * Returns the booking date for the journal entry.
     * If null, the current date/time will be used.
     * 
     * @return the booking date or null if not specified
     */
    default OffsetDateTime getJournalBookingDate() {
        return null;
    }

    /**
     * Returns additional journal information for this template.
     * 
     * @return journal info list or null if not applicable
     */
    default List<JournalInfoDTO> journalInfo() {
        return null;
    }

    /**
     * Builds the journal lines for this template.
     * 
     * @param journalId the journal ID to associate with the lines
     * @return list of journal lines
     * @throws AccountingMakerException if journal lines cannot be built
     */
    List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException;

    /**
     * @return true if this template is for a test account
     */
    boolean isTestAccount();

    /**
     * Indicates whether a new transaction should be created.
     * 
     * @return true if a new transaction should be created, false to link to existing
     */
    default boolean addNewTx() {
        return true;
    }

    /**
     * Returns the list of transaction IDs to link to this journal.
     * Only applicable when {@link #addNewTx()} returns false.
     * 
     * @return list of transaction IDs to link (empty if none)
     */
    @NotNull
    default List<Long> linkTxs() {
        return Collections.emptyList();
    }

    /**
     * Builds the transaction for this template.
     * Must be implemented if {@link #addNewTx()} returns true.
     * 
     * @return the transaction to create
     * @throws UnsupportedOperationException if this template doesn't support transaction creation
     */
    default Tx buildTx() {
        throw new UnsupportedOperationException(String.format(
                "Operation buildTx() is not supported for template class '%s'. " +
                "Override this method if addNewTx() returns true.",
                this.getClass().getSimpleName()));
    }

    /**
     * Builds specialized transactions for this template.
     * Must be implemented if {@link #addSpecializedTx()} returns true.
     * 
     * @param txId the transaction ID to associate with specialized transactions
     * @return list of specialized transactions
     * @throws AccountingMakerException if specialized transactions cannot be built
     * @throws UnsupportedOperationException if this template doesn't support specialized transactions
     */
    default List<SpecializedTx> buildSpecializedTxs(long txId) throws AccountingMakerException {
        throw new UnsupportedOperationException(String.format(
                "Operation buildSpecializedTxs() is not supported for template class '%s'. " +
                "Override this method if addSpecializedTx() returns true.",
                this.getClass().getSimpleName()));
    }

    /**
     * Updates specialized transactions after journal creation.
     * Override to perform post-creation updates.
     * 
     * @throws AccountingMakerException if update fails
     */
    default void updateSpecializedTx() throws AccountingMakerException {
        // nothing to do by default
    }

    /**
     * Indicates whether specialized transactions should be created.
     * 
     * @return true if specialized transactions should be created
     */
    default boolean addSpecializedTx() {
        return false;
    }

    /**
     * Processes this template by creating the journal.
     * This is the main entry point for template execution.
     * 
     * @throws AccountingMakerException if journal creation fails
     */
    default void process() throws AccountingMakerException {
        getJournalMaker().createJournal(this);
    }
}
