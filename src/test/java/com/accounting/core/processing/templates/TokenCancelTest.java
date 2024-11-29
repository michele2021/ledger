package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.exceptions.EventAlreadyProcessedException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalDTO;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.JournalLineDTO;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenCancelTest {

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

    @Mock
    JournalDTO journal;

    RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();
    JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);

    @BeforeEach
    void init() {
        when(message.getAdditionalData()).thenReturn(additionalData);
    }


    @Test
    void shouldConstructorValidateIfTxReferencePresent() {
        //given
        when(message.getTxReference()).thenReturn(null);

        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> new TokenCancel(
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

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Activated.toString());
        when(txStatus.getAmount()).thenReturn(amount);
        when(txStatus.getTxId()).thenReturn(123456L);

        TokenCancel subject = new TokenCancel(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when
        subject.process();

        //then
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReferenceAndAccountId(eq("TxReference"), eq(1L));
        verify(journalMaker).createJournal(same(subject));

        assertThat(subject.getTxId()).isEqualTo(123456L);
    }

    @Test
    void shouldProcessFailIfReferenceIsNotFound() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(null);

        TokenCancel subject = new TokenCancel(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Tx not found for tx reference: " + "TxReference" + " and accountId: 0");
    }

    @Test
    void shouldProcessFailForCancelledJournalType() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Cancelled.toString());

        TokenCancel subject = new TokenCancel(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        EventAlreadyProcessedException exception = assertThrows(EventAlreadyProcessedException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Processing 'Cancel' message. Tx 0 is in status Cancelled (journalId: 0). Skipping the message without processing.");
    }

    @Test
    void shouldProcessFailForConvertedJournalType() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Converted.toString());

        TokenCancel subject = new TokenCancel(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Processing 'Cancel' message. Tx 0 is in status 'Converted' cannot be cancelled.");
    }

    @Test
    void shouldProcessFailForInvalidAmount() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Activated.toString());
        when(txStatus.getAmount()).thenReturn(Amount.of(456, 12));

        TokenCancel subject = new TokenCancel(
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
        TokenCancel subject = new TokenCancel(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when & then
        assertThat(subject.addNewTx()).isFalse();
    }

    @Test
    @Disabled
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
        final int registerTypeTokenCancelRevenueId = 52324;

        final long registerTypeTokenExpiredBatchId = registerTypeTokenExpiredId * 100;
        final long registerTypeTokenActivatedBatchId = registerTypeTokenActivatedId * 100;
        final long registerTypeCardsPayablePendingBatchId = registerTypeCardsPayablePendingId * 100;
        final long registerTypeTokenCancelRevenueBatchId = registerTypeTokenCancelRevenueId * 100;

        // TODO to be completed
        JournalLineDTO jld = new JournalLineDTO();
        List<JournalLineDTO> journalLinesDTO = List.of(jld);

        when(message.getTxReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        // This seems to not work properly (we get NPE)
        when(repositoryHelper.getJournalDAO().getLatestJournalByTxReferenceAndJournalTypeCode(anyString(), anyString())).thenReturn(journal);

        when(journal.getJournalLines()).thenReturn(journalLinesDTO);
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
        when(repositoryHelper.getRegisterTypeRepo().get("TokenCancelRevenue")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenCancelRevenueId).build());


        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenActivatedId, accountId)).thenReturn(registerTypeTokenActivatedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenExpiredId, accountId)).thenReturn(registerTypeTokenExpiredBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenCancelRevenueId, revenueAccountId)).thenReturn(registerTypeTokenCancelRevenueBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId)).thenReturn(registerTypeCardsPayablePendingBatchId);


        List<JournalLine> expectedJournalLines = Arrays.asList(
                JournalLine.builder().batchId(registerTypeTokenExpiredBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenActivatedBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeCardsPayablePendingBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenCancelRevenueBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build()
        );


        TokenCancel subject = new TokenCancel(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );
        subject.process();


        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenActivatedId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenExpiredId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenCancelRevenueId, revenueAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId);

        assertThat(result).isEqualTo(expectedJournalLines);

    }

    @Test
    void shouldUseAdditionalDataBookingDate() throws Exception {
        //given
        String bookingDateAsString = "2022-05-15T11:38:32Z";

        additionalData = new Gson().fromJson("{ " +
                "\"bookingDate\" : \"" + bookingDateAsString + "\"" +
                "}", JsonObject.class);
        when(message.getAdditionalData()).thenReturn(additionalData);
        when(message.getTxReference()).thenReturn("txRef1");
        TxVariant txv = new TxVariant();
        txv.setTxVariantCode("BASE");
        txv.setTxVariantId(1);
        when(repositoryHelper.getTxVariantRepo().get(anyString())).thenReturn(txv);

        TokenCancel subject = new TokenCancel(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper);

        //when
        OffsetDateTime bd = subject.getJournalBookingDate();

        //then
        assertThat(bd.toString()).isEqualTo(bookingDateAsString);
    }

    @Test
    void shouldFailWhenBookingDateInTheFuture() {
        //given
        additionalData = new Gson().fromJson("{ " +
                "\"bookingDate\" : \"2028-12-30T11:00:00.00Z\"" +
                "}", JsonObject.class);
        when(message.getAdditionalData()).thenReturn(additionalData);
        when(message.getTxReference()).thenReturn("txRef1");

        // when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class,
                () -> new TokenCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData: the 'bookingDate' cannot be in the future");
    }
}
