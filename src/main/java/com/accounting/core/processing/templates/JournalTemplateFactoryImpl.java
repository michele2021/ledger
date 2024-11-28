package com.accounting.core.processing.templates;

import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalTemplate;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.lang.String.format;

public class JournalTemplateFactoryImpl implements JournalTemplateFactory {

    private final JournalMaker journalMaker;
    private final RepositoryHelper repositoryHelper;

    public JournalTemplateFactoryImpl(JournalMaker journalMaker, RepositoryHelper repositoryHelper) {
        this.journalMaker = journalMaker;
        this.repositoryHelper = repositoryHelper;
    }

    @Override
    public JournalTemplateIf makeJournalTemplateInstance(QueueMessage message, JournalTemplate journalTemplate, Account account, Amount amount, JournalType jt, TxType txType) throws AccountingTemplateException {
        try {
            Class<?> clazz = Class.forName("com.accounting.core.processing.templates." + journalTemplate.getClazz());
            Constructor<?> constructor = clazz.getConstructor(JournalMaker.class, Account.class, Amount.class, JournalType.class, TxType.class, QueueMessage.class, RepositoryHelper.class);
            return (JournalTemplateIf) constructor.newInstance(journalMaker, account, amount, jt, txType, message, repositoryHelper);

        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new AccountingTemplateException(format("Exception while trying to instantiate class %s for the template %s",
                    journalTemplate.getClazz(), journalTemplate.getFileName()), e);
        }
    }

}
