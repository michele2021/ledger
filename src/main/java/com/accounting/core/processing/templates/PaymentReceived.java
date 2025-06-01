package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.commons.repository.JournalTypeRepoIf;
import com.accounting.commons.repository.PaymentProviderRepoIf;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Payment;
import com.accounting.core.entities.SpecializedTx;
import com.accounting.core.entities.Tx;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;

import java.util.ArrayList;
import java.util.List;

public class PaymentReceived extends AbstractTemplate {
    private Tx oriTx;
    private final Payment payment;
    private final PaymentProviderRepoIf paymentProviderRepo;
    private final JournalTypeRepoIf journalTypeRepo;
    private final String oriTxReference;

    public PaymentReceived(JournalMaker journalMaker,
                           Account account,
                           Amount amount,
                           JournalType journalType,
                           TxType txType,
                           QueueMessage message,
                           RepositoryHelper repositoryHelper) throws AccountingTemplateException {
        super(txType, account, message.uuid(), journalType, journalMaker, amount, message.additionalData(), repositoryHelper);
        this.oriTxReference = message.txReference();
        this.payment = gson.fromJson(additionalData, Payment.class);
        this.paymentProviderRepo = repositoryHelper.getPaymentProviderRepo();
        this.journalTypeRepo = repositoryHelper.getJournalTypeRepo();
        validateIfTxReferencePresent();
    }

    @Override
    public void process() throws AccountingMakerException {
        // reconciliation
        oriTx = journalDAO.getTxByReference(oriTxReference);
        // TODO if the match is not found, the payment should still be created (as not matched)
        // throwExceptionIfNull(oriTx, format("Tx not found with oriTxReference '%s'.", oriTxReference));
        journalMaker.createJournal(this);
    }

    @Override
    public JournalType getJournalType() {
        if(oriTx == null) {
            // payment isn't matched (yet)
            return super.getJournalType();
        }
        return journalTypeRepo.get(Constants.JournalType.PaymentMatched.name());
    }

    @Override
    public Tx buildTx() {
        return buildBaseTx();
    }

    @Override
    public boolean addSpecializedTx() {
        return true;
    }

    @Override
    public List<SpecializedTx> buildSpecializedTxs(long txId) {
        var paymentProvider = paymentProviderRepo.get(payment.paymentProvider().toString());
        var paymentDTO = payment.paymentDTO(txId, paymentProvider.getID());
        return List.of(paymentDTO);
    }

    @Override
    public Long linkTx() {
        if(oriTx == null) {
            return null;
        }
        return oriTx.getTxId();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {
        var journalLines = new ArrayList<JournalLine>();

        long accountId = account.getAccountId();
        addDebitLine(journalId, journalLines, Constants.RegisterType.Clearing, accountId, amount);
        var creditRegisterType = oriTx == null ?
                Constants.RegisterType.UnappliedCash : Constants.RegisterType.Receivable;
        addCreditLine(journalId, journalLines, creditRegisterType, accountId, amount);
        return journalLines;
    }
}
