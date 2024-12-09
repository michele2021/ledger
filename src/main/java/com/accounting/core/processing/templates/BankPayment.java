package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.accounting.commons.Message.AdditionalData.accountIdFrom;
import static com.accounting.commons.Message.AdditionalData.accountIdTo;

@Slf4j
public class BankPayment extends AbstractTemplate {

    private long beneficiaryAccountId;
    private long senderAccountId;

    public BankPayment(JournalMaker journalMaker,
                       Account account,
                       Amount amount,
                       JournalType journalType,
                       TxType txType,
                       QueueMessage message,
                       RepositoryHelper repositoryHelper) throws AccountingMakerException {
        // message.getUuid() is the requestId coming from Monavate
        super(txType, account, message.getUuid(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        validateAndGetAdditionalData();
    }

    @Override
    public Tx buildTx() {
        Tx tx = buildBaseTx();
        String txDescription = getAdditionalData(Message.AdditionalData.description, JsonElement::getAsString);
        String desc = txDescription == null ? null : txDescription.substring(0, Math.min(txDescription.length(), 255));
        tx.setDescription(desc);
        String txRef = txType.getCode().substring(0, 3).toUpperCase().concat("_").concat(txReference);
        tx.setTxReference(txRef);
        return tx;
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        List<JournalLine> jls = new ArrayList<>();

        // Debit/Credit
        addDebitLine(journalId, jls, getRegisterTypeId(Constants.RegisterType.BankBalance), beneficiaryAccountId, amount);
        addCreditLine(journalId, jls, getRegisterTypeId(Constants.RegisterType.BankBalance), senderAccountId, amount);
        return jls;
    }

    private void validateAndGetAdditionalData() throws AccountingMakerException {
        Account account;

        senderAccountId = getAdditionalData(accountIdFrom).getAsLong();
        account = accountRepo.get(senderAccountId);
        throwExceptionIfNull(account, "Account not found with accountId " + senderAccountId);

        beneficiaryAccountId = getAdditionalData(accountIdTo).getAsLong();
        account = accountRepo.get(beneficiaryAccountId);
        throwExceptionIfNull(account, "Account not found with accountId " + beneficiaryAccountId);
    }

}