package com.accounting.core.processing.templates;


import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.entities.TxStatus;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
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
class CardAccountReloadTest {

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

    @Mock
    Account issuerAccount;


    RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    final String prepaidCardExternalId =  "prepaidCardExternalId";
    JsonObject additionalData = new Gson().fromJson("{ \"issuer\" : \"issuer\", \"prepaidCardExternalId\" : \"" + prepaidCardExternalId +"\"}", JsonObject.class);

    @BeforeEach
    void init() {
        when(message.getUuid()).thenReturn("TxReference");
        when(message.getAdditionalData()).thenReturn(additionalData);
    }

    @Test
    void shouldConstructorValidateIfTxReferencePresent() {
        //given
        when(message.getUuid()).thenReturn(null);

        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> new CardAccountReload(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        ));

        //then
        assertThat(exception.getMessage()).isEqualTo("Mandatory original txReference (cardTokenId) not found in the queue message.");
    }

    @Test
    void shouldProcessOK() throws Exception {
        //given
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(prepaidCardExternalId)).thenReturn(txStatus);
        when(txStatus.getTxId()).thenReturn(123456L);

        when(message.getAdditionalData()).thenReturn(additionalData);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(issuerAccount);

        CardAccountReload subject = new CardAccountReload(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when
        subject.process();

        //then
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReference(eq(prepaidCardExternalId));
        verify(repositoryHelper.getAccountRepo()).getByCodeAndType(eq("issuer"), eq(Constants.AccountType.Issuer.toString()));
        verify(journalMaker).createJournal(same(subject));

        assertThat(subject.getTxId()).isEqualTo(123456L);
    }

    @Test
    void shouldProcessFailIfReferenceIsNotFound() throws Exception {
        //given
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(prepaidCardExternalId)).thenReturn(null);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(issuerAccount);

        CardAccountReload subject = new CardAccountReload(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Tx not found for reload with prepaid card id: "+ prepaidCardExternalId);
    }

    @Test
    void shouldNotCreateNewTx() throws Exception {
        //given
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(issuerAccount);
        CardAccountReload subject = new CardAccountReload(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when & then
        assertThat(subject.addNewTx()).isFalse();
    }

    @Test
    void shouldProcessFailIfIssuerAccountNotFound() throws Exception {
        //given
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(prepaidCardExternalId)).thenReturn(txStatus);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(null);

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, () -> new CardAccountReload(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        ));

        //then
        assertThat(exception.getMessage()).contains("'issuer' issuer not found.");
    }

    @Test
    void shouldBuildJournalLines() throws Exception {
        //given
        final long journalId = 1111L;
        final long accountId = 2222L;
        final long issuerAccountId = 33333L;
        final long cardReloadAccountId = 44444L;
        final int currencyId = 342;


        final int registerTypeOpenBalanceId = 10001;
        final int registerTypeCardAccountBalanceId = 10002;
        final int registerTypeIssuerPrefundAccountId = 10003;
        final int registerTypeCardsPayableId = 10004;

        final long registerTypeOpenBalanceBatchId = registerTypeOpenBalanceId * 100;
        final long registerTypeCardAccountBalanceBatchId = registerTypeCardAccountBalanceId * 100;
        final long registerTypeIssuerPrefundAccountBatchId = registerTypeIssuerPrefundAccountId * 100;
        final long registerTypeCardsPayableBatchId = registerTypeCardsPayableId * 100;


        when(amount.getQuantity()).thenReturn(1000L);
        when(amount.getCurrencyId()).thenReturn(currencyId);
        when(repositoryHelper.getJournalDAO().getTxStatusByReference(prepaidCardExternalId)).thenReturn(txStatus);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(issuerAccount);
        when(account.getAccountId()).thenReturn(accountId);
        when(issuerAccount.getAccountId()).thenReturn(issuerAccountId);
        when(txStatus.getAccountId()).thenReturn(cardReloadAccountId);

        when(repositoryHelper.getRegisterTypeRepo().get("OpenBalance")).thenReturn(RegisterType.builder().registerTypeId(registerTypeOpenBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("CardAccountBalance")).thenReturn(RegisterType.builder().registerTypeId(registerTypeCardAccountBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("IssuerPrefundAccount")).thenReturn(RegisterType.builder().registerTypeId(registerTypeIssuerPrefundAccountId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("CardsPayable")).thenReturn(RegisterType.builder().registerTypeId(registerTypeCardsPayableId).build());

        when(journalMaker.getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId)).thenReturn(registerTypeOpenBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeCardAccountBalanceId, cardReloadAccountId)).thenReturn(registerTypeCardAccountBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeIssuerPrefundAccountId, issuerAccountId)).thenReturn(registerTypeIssuerPrefundAccountBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeCardsPayableId, issuerAccountId)).thenReturn(registerTypeCardsPayableBatchId);

        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeOpenBalanceBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeCardAccountBalanceBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeIssuerPrefundAccountBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeCardsPayableBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build()
                );


        CardAccountReload subject = new CardAccountReload(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );
        subject.process();


        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeCardAccountBalanceId, cardReloadAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeIssuerPrefundAccountId, issuerAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeCardsPayableId, issuerAccountId);

        assertThat(result).isEqualTo(expectedJournalLines);

    }

    @Test
    void shouldFailWhenBookingDateInTheFuture() {
        //given
        additionalData = new Gson().fromJson("{ " +
                "\"issuer\" : \"1111\"," +
                "\"bookingDate\" : \"2099-12-30T11:00:00.00Z\"," +
                "\"prepaidCardExternalId\" : prepaidCardExternalId\"" +
                "}", JsonObject.class);
        when(message.getAdditionalData()).thenReturn(additionalData);

        // when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class,
                () -> new CardAccountReload(journalMaker, account, amount, journalType, txType, message, repositoryHelper));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData: the 'bookingDate' cannot be in the future");
    }

}