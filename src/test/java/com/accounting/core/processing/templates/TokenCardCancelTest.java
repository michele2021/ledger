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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokenCardCancelTest {

    @Mock
    JournalMaker journalMaker;

    @Mock
    Account account;

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

    private final JsonObject additionalData = new Gson().fromJson("{ \"remainingBalance\" : 10 }", JsonObject.class);

    @Test
    void shouldConstructorValidateIfTxReferencePresent() {
        //given
        when(message.getTxReference()).thenReturn(null);

        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> new TokenCardCancel(
                journalMaker, account, amount, journalType, txType, message, repositoryHelper));

        //then
        assertThat(exception.getMessage()).isEqualTo("Mandatory original txReference (cardTokenId) not found in the queue message.");
    }

    @Test
    void shouldProcessOK() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        when(message.getAdditionalData()).thenReturn(additionalData);
        when(account.getAccountId()).thenReturn(1L);

        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Converted.toString());
        when(txStatus.getAmount()).thenReturn(amount);
        when(txStatus.getTxId()).thenReturn(123456L);

        TokenCardCancel subject = new TokenCardCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper);

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
        when(message.getAdditionalData()).thenReturn(additionalData);

        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(null);

        TokenCardCancel subject = new TokenCardCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper);

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Tx not found for tx reference: TxReference and accountId: 0");
    }

    @Test
    void shouldProcessFailForExpiredJournalType() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        when(message.getAdditionalData()).thenReturn(additionalData);

        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.CardCancelled.toString());

        TokenCardCancel subject = new TokenCardCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper);

        //when
        EventAlreadyProcessedException exception = assertThrows(EventAlreadyProcessedException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Processing 'CardCancel' message. Tx 0 is in status CardCancelled (journalId: 0). Skipping the message without processing.");
    }


    @Test
    void shouldProcessFailForAnyOtherThanConvertedJournalType() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        when(message.getAdditionalData()).thenReturn(additionalData);
        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Activated.toString());

        TokenCardCancel subject = new TokenCardCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper);

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Processing 'TokenCardCancel' message. Tx 0 is in status 'Activated', but the only compatible status is 'Converted'.");
    }

    @Test
    void shouldProcessFailForInvalidAmount() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        when(message.getAdditionalData()).thenReturn(additionalData);
        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Converted.toString());
        when(txStatus.getAmount()).thenReturn(Amount.of(456, 12));

        TokenCardCancel subject = new TokenCardCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper);

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).contains("The amount specified in the message amount does not match with the amount in the original tx");
    }

    @Test
    void shouldNotCreateNewTx() throws Exception {
        //given
        when(message.getTxReference()).thenReturn("TxReference");
        when(message.getAdditionalData()).thenReturn(additionalData);
        TokenCardCancel subject = new TokenCardCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper);

        //when & then
        assertThat(subject.addNewTx()).isFalse();
    }


    @Test
    void shouldBuildJournalLines() throws Exception {
        //given
        final long journalId = 8921738921L;
        final long accountId = account.getAccountId();
        final long issuerAccountId = 1L;
        final int currencyId = 342;

        final int registerTypeTokenCardCancelledId = 5471;
        final int registerTypeOpenBalanceId = 7357832;
        final int registerTypeCardsPayablePendingId = 5234;
        final int registerTypeCardsPayableId = 52324;

        final long registerTypeTokenCardCancelledBatchId = registerTypeTokenCardCancelledId * 100;
        final long registerTypeOpenBalanceBatchId = registerTypeOpenBalanceId * 100;
        final long registerTypeCardsPayablePendingBatchId = registerTypeCardsPayablePendingId * 100;
        final long registerTypeCardsPayableBatchId = registerTypeCardsPayableId * 100;

        when(message.getTxReference()).thenReturn("TxReference");
        when(message.getAdditionalData()).thenReturn(additionalData);
        when(repositoryHelper.getJournalDAO().getTxStatusWithIssuerAccountByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(repositoryHelper.getCurrencyRepo().get(anyLong())).thenReturn(Currency.builder().currencyCode(Constants.Currencies.EUR.toString()).currencyId(currencyId).build());
        when(txStatus.getIssuerAccountId()).thenReturn(issuerAccountId);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Converted.toString());
        when(txStatus.getAmount()).thenReturn(amount);
        when(amount.getCurrencyId()).thenReturn(currencyId);

        when(repositoryHelper.getRegisterTypeRepo().get("TokenCardCancelled")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenCardCancelledId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("OpenBalance")).thenReturn(RegisterType.builder().registerTypeId(registerTypeOpenBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("CardsPayablePending")).thenReturn(RegisterType.builder().registerTypeId(registerTypeCardsPayablePendingId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("CardsPayable")).thenReturn(RegisterType.builder().registerTypeId(registerTypeCardsPayableId).build());

        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenCardCancelledId, accountId)).thenReturn(registerTypeTokenCardCancelledBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId)).thenReturn(registerTypeOpenBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId)).thenReturn(registerTypeCardsPayablePendingBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeCardsPayableId, issuerAccountId)).thenReturn(registerTypeCardsPayableBatchId);

        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeOpenBalanceBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenCardCancelledBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeCardsPayablePendingBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeCardsPayableBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build());

        TokenCardCancel subject = new TokenCardCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper);
        subject.process();

        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenCardCancelledId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeCardsPayableId, issuerAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId);

        AssertionsForInterfaceTypes.assertThat(result).isEqualTo(expectedJournalLines);
    }

    @Test
    void shouldUseAdditionalDataBookingDate() throws Exception {
        //given
        String bookingDateAsString = "2022-05-15T11:38:32Z";

        JsonObject additionalData = new Gson().fromJson("{ " +
                "\"bookingDate\" : \"" + bookingDateAsString + "\"," +
                "\"remainingBalance\" : 10" +
                "}", JsonObject.class);
        when(message.getAdditionalData()).thenReturn(additionalData);
        when(message.getTxReference()).thenReturn("txRef1");
        TxVariant txv = new TxVariant();
        txv.setTxVariantCode("BASE");
        txv.setTxVariantId(1);
        when(repositoryHelper.getTxVariantRepo().get(anyString())).thenReturn(txv);

        TokenCardCancel subject = new TokenCardCancel(
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
        JsonObject additionalData = new Gson().fromJson("{ " +
                "\"bookingDate\" : \"2028-12-30T11:00:00.00Z\"," +
                "\"remainingBalance\" : 10" +
                "}", JsonObject.class);
        when(message.getAdditionalData()).thenReturn(additionalData);
        when(message.getTxReference()).thenReturn("txRef1");

        // when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class,
                () -> new TokenCardCancel(journalMaker, account, amount, journalType, txType, message, repositoryHelper));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData: the 'bookingDate' cannot be in the future");
    }
}