package com.accounting.core.processing.templates;

import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.exceptions.AccountingTemplateException;

/**
 * Delegate act of JournalTemplateIf implementation creation, mostly to allow painless unit testing.
 */
public interface JournalTemplateFactory {

    JournalTemplateIf makeJournalTemplateInstance(
            QueueMessage message,
            JournalTemplate journalTemplate,
            Account account,
            Amount amount,
            JournalType jt,
            TxType txType) throws AccountingTemplateException;

}
