package com.accounting.core.processing.templates;

import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.repository.RegisterTypeRepoIf;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalLinesMaker;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.reports.model.DailySalesReportRow;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Slf4j
public class TokenMatch extends JournalLinesMaker {

    @Getter
    protected final JournalType journalType;

    private final Account account;
    private final Amount saleAmount;
    private final JournalDAO journalDAO;
    private final Tx tokenReconciliationTx;
    private final DailySalesReportRow reportRow;

    public TokenMatch(Account account,
                      JournalDAO journalDAO,
                      JournalType journalType,
                      Tx tokenReconciliationTx,
                      JournalMaker journalMaker,
                      RegisterTypeRepoIf registerTypeRepo,
                      DailySalesReportRow reportRow) {
        super(journalMaker, registerTypeRepo);
        this.tokenReconciliationTx = tokenReconciliationTx;
        this.account = account;
        this.reportRow = reportRow;
        this.journalDAO = journalDAO;
        this.journalType = journalType;
        this.saleAmount = new Amount(reportRow.getGrossAmountQuantity(), reportRow.getCurrency());
    }

    @Override
    public boolean addNewTx() {
        return false;
    }

    @Override
    public List<Long> linkTxs() {
        return List.of(tokenReconciliationTx.getTxId());
    }

    @Override
    public void process() throws AccountingMakerException {
        journalMaker.createJournal(this);
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) {

        long accountId = account.getAccountId();
        var journalLines = new ArrayList<JournalLine>();

        // tokenReconciliationTx amount includes fees
        Amount tokenAmount = tokenReconciliationTx.getAmount();

        Amount receivedFee = journalDAO.getReceivedFee(tokenReconciliationTx.getParentTxId(), reportRow.getJournalType());
        if (nonNull(receivedFee)) {

            // Credit
            //addCreditLine(journalId, journalLines, Constants.RegisterType.FeeMatched, accountId, receivedFee);

            Amount txAmount = tokenReconciliationTx.getAmount();

            // in case of Cancellation the value is negative
            long quantity = txAmount.getQuantity() - Math.abs(receivedFee.getQuantity());
            tokenAmount = Amount.of(quantity, txAmount.getCurrencyId());
        }

        // Credit
        //addCreditLine(journalId, journalLines, Constants.RegisterType.TokenMatched, accountId, tokenAmount);

        // Debit
        // N.B. saleAmount can also be calculated ad receivedFee + tokenAmount
        //addDebitLine(journalId, journalLines, Constants.RegisterType.SaleMatched, accountId, saleAmount);

        return journalLines;
    }

    @Override
    public boolean isTestAccount() {
        return account.isTest();
    }

}
