package com.accounting.core.processing.templates;


import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.EventAlreadyProcessedException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenExpiryTest {

    @Mock
    JournalMaker journalMaker;

    @Mock
    Account account;

    @Mock
    Account revenueAccount;

    @Mock
    Amount amount;

    @Mock
    JournalType journalType;

    @Mock
    TxType txType;

    @Mock
    QueueMessage message;

    @Mock
    TxStatus txStatus;

    RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    @Test
    void shouldConstructorValidateIfTxReferencePresent() {
        //given
        when(message.getTxReference()).thenReturn(null);

        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> new TokenExpiry(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        ));

        //then
        assertThat(exception.getMessage()).isEqualTo("Mandatory original txReference (cardTokenId) not found in the queue message.");
    }

    @Test
    void shouldProcessOK() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        when(account.getAccountId()).thenReturn(1L);

        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Activated.toString());
        when(txStatus.getAmount()).thenReturn(amount);
        when(txStatus.getTxId()).thenReturn(123456L);

        TokenExpiry subject = new TokenExpiry(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        subject.process();

        //then
        verify(repositoryHelper.getJournalDAO()).getTxStatusWithIssuerAccountByReferenceAndAccountId(eq("TxReference"), eq(1L));
        verify(journalMaker).createJournal(same(subject));

        assertThat(subject.getTxId()).isEqualTo(123456L);
    }

    @Test
    void shouldProcessFailIfReferenceIsNotFound() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(null);

        TokenExpiry subject = new TokenExpiry(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Tx not found for tx reference: TxReference and accountId: 0");
    }

    @Test
    void shouldProcessFailForExpiredJournalType() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Expired.toString());

        TokenExpiry subject = new TokenExpiry(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        EventAlreadyProcessedException exception = assertThrows(EventAlreadyProcessedException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Processing 'Expiry' message. Tx 0 is in status Expired (journalId: 0). Skipping the message without processing.");
    }


    @Test
    void shouldProcessFailForAnyOtherThanActivatedJournalType() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Cancelled.toString());

        TokenExpiry subject = new TokenExpiry(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Processing 'Expiry' message. Tx 0 is in status 'Cancelled' cannot be expired.");
    }

    @Test
    void shouldProcessFailForInvalidAmount() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Activated.toString());
        when(txStatus.getAmount()).thenReturn(Amount.of(456, 12));

        TokenExpiry subject = new TokenExpiry(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).contains("The amount specified in the message amount does not match with the amount in the original tx");
    }

    @Test
    void shouldNotCreateNewTx() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        TokenExpiry subject = new TokenExpiry(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when & then
        assertThat(subject.addNewTx()).isFalse();
    }


    @Test
    void shouldBuildJournalLines() throws Exception {
        //given
        final long journalId = 8921738921L;
        final long accountId = 756863L;
        final long issuerAccountId = 1L;
        final long revenueAccountId = 2L;
        final int currencyId = 342;

        final int registerTypeTokenExpiredId = 5471;
        final int registerTypeTokenActivatedId = 7357832;
        final int registerTypeCardsPayablePendingId = 5234;
        final int registerTypeTokenExpiryRevenueId = 52324;

        final long registerTypeTokenExpiredBatchId = registerTypeTokenExpiredId * 100;
        final long registerTypeTokenActivatedBatchId = registerTypeTokenActivatedId * 100;
        final long registerTypeCardsPayablePendingBatchId = registerTypeCardsPayablePendingId * 100;
        final long registerTypeTokenExpiryRevenueBatchId = registerTypeTokenExpiryRevenueId * 100;

        when(message.getTxReference()).thenReturn("TxReference");
        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getIssuerAccountId()).thenReturn(issuerAccountId);
        when(txStatus.getAccountId()).thenReturn(accountId);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Activated.toString());
        when(txStatus.getAmount()).thenReturn(amount);
        when(amount.getQuantity()).thenReturn(1000L);
        when(amount.getCurrencyId()).thenReturn(currencyId);

        when(repositoryHelper.getAccountRepo().getByCodeAndType("Ledger", Constants.AccountType.BusinessEntity.name())).thenReturn(revenueAccount);
        when(revenueAccount.getAccountId()).thenReturn(revenueAccountId);

        when(repositoryHelper.getRegisterTypeRepo().get("TokenExpired")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenExpiredId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("TokenActivated")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenActivatedId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("CardsPayablePending")).thenReturn(RegisterType.builder().registerTypeId(registerTypeCardsPayablePendingId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("TokenExpiryRevenue")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenExpiryRevenueId).build());


        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenActivatedId, accountId)).thenReturn(registerTypeTokenActivatedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenExpiredId, accountId)).thenReturn(registerTypeTokenExpiredBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenExpiryRevenueId, revenueAccountId)).thenReturn(registerTypeTokenExpiryRevenueBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId)).thenReturn(registerTypeCardsPayablePendingBatchId);


        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeTokenExpiredBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenActivatedBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeCardsPayablePendingBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenExpiryRevenueBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build()
        );


        TokenExpiry subject = new TokenExpiry(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );
        subject.process();


        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenActivatedId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenExpiredId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenExpiryRevenueId, revenueAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId);

        assertThat(result).isEqualTo(expectedJournalLines);

    }

}