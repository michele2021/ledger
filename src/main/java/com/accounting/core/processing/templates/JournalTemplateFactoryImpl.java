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

    private static final String TEMPLATE_PACKAGE = "com.accounting.core.processing.templates.";
    private final JournalMaker journalMaker;
    private final RepositoryHelper repositoryHelper;

    public JournalTemplateFactoryImpl(JournalMaker journalMaker, RepositoryHelper repositoryHelper) {
        this.journalMaker = journalMaker;
        this.repositoryHelper = repositoryHelper;
    }

    @Override
    public JournalTemplateIf makeJournalTemplateInstance(QueueMessage message, JournalTemplate journalTemplate,
                                                         Account account, Amount amount, JournalType jt, TxType txType)
            throws AccountingTemplateException {
        String className = journalTemplate.getClazz();
        try {
            Constructor<?> constructor = loadConstructor(className);
            return (JournalTemplateIf) constructor.newInstance(journalMaker, account, amount, jt, txType, message, repositoryHelper);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new AccountingTemplateException(format(
                    "Error during instantiation of template class '%s' (template file: '%s'). Cause: %s",
                    className, journalTemplate.getFileName(), cause != null ? cause.getMessage() : "Unknown"), e);
        } catch (InstantiationException e) {
            throw new AccountingTemplateException(format(
                    "Cannot instantiate abstract class '%s' for template '%s'. Ensure the class is concrete.",
                    className, journalTemplate.getFileName()), e);
        } catch (IllegalAccessException e) {
            throw new AccountingTemplateException(format(
                    "Constructor not accessible for class '%s' (template: '%s'). Ensure constructor is public.",
                    className, journalTemplate.getFileName()), e);
        } catch (ClassCastException e) {
            throw new AccountingTemplateException(format(
                    "Class '%s' does not implement JournalTemplateIf interface (template: '%s').",
                    className, journalTemplate.getFileName()), e);
        }
    }

    private Constructor<?> loadConstructor(String className) throws AccountingTemplateException {
        try {
            Class<?> clazz = Class.forName(TEMPLATE_PACKAGE + className);
            return clazz.getConstructor(JournalMaker.class, Account.class, Amount.class, 
                    JournalType.class, TxType.class, QueueMessage.class, RepositoryHelper.class);
        } catch (ClassNotFoundException e) {
            throw new AccountingTemplateException(format(
                    "Template class '%s' not found in package '%s'. Verify the class name and package.",
                    className, TEMPLATE_PACKAGE), e);
        } catch (NoSuchMethodException e) {
            throw new AccountingTemplateException(format(
                    "Required constructor not found for class '%s'. Expected: (JournalMaker, Account, Amount, JournalType, TxType, QueueMessage, RepositoryHelper).",
                    className), e);
        }
    }
}
