package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.commons.repository.CurrencyRepoIf;
import com.accounting.core.dto.JournalPostingDTO;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.Tx;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.processing.OffsetDateTimeAdapter;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JournalPost extends AbstractTemplate {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();
    private final JournalPostingDTO journal;
    private final CurrencyRepoIf currencyRepoIf;

    public JournalPost(JournalMaker journalMaker, Account account, Amount amount, JournalType journalType, TxType txType,
                       QueueMessage message,
                       RepositoryHelper repositoryHelper) {
        super(txType, account, message.uuid(), journalType, journalMaker, amount, message.additionalData(), repositoryHelper);
        journal = gson.fromJson(additionalData, JournalPostingDTO.class);
        this.currencyRepoIf = repositoryHelper.getCurrencyRepo();
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        var date = LocalDate.parse(journal.bookingDate());
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    @Override
    public Tx buildTx() {
        return buildBaseTx();
    }

    @Override
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        var journalLines = new ArrayList<JournalLine>();
        for (var jl : journal.journalLines()) {
            var currency = currencyRepoIf.get(jl.currency().name());
            var amount = Amount.of(Constants.toMinorUnits(jl.currency(), jl.amount()), currency.getCurrencyId());
            if (jl.lineType() == Constants.LineType.DEBIT) {
                addDebitLine(journalId, journalLines, jl.registerType(), account.getAccountId(), amount);
            } else {
                addCreditLine(journalId, journalLines, jl.registerType(), account.getAccountId(), amount);
            }
        }
        return journalLines;
    }
}
