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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenActivateTest {

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
    JsonObject additionalData = new Gson().fromJson("{}", JsonObject.class);

    @BeforeEach
    void init() {
        when(message.additionalData()).thenReturn(additionalData);
    }

    @Test
    void shouldConstructorValidateIfTxReferencePresent() {
        //given
        when(message.txReference()).thenReturn(null);

        //when
        AccountingTemplateException exception = assertThrows(AccountingTemplateException.class, () -> new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        ));

        //then
        assertThat(exception.getMessage()).isEqualTo("Mandatory original txReference (cardTokenId) not found in the queue message.");
    }

    @Test
    void shouldProcessOK() throws Exception {
        //given
        when(message.txReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Created.toString());
        when(txStatus.getAmount()).thenReturn(amount);
        when(txStatus.getTxId()).thenReturn(123456L);

        JsonObject additionalData = new Gson().fromJson("{ \"issuer\" : \"issuer\"}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(issuerAccount);


        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        subject.process();

        //then
        verify(repositoryHelper.getJournalDAO()).getTxStatusByReferenceAndAccountId(eq("TxReference"), anyLong());
        verify(repositoryHelper.getAccountRepo()).getByCodeAndType(eq("issuer"), eq(Constants.AccountType.Issuer.toString()));
        verify(journalMaker).createJournal(same(subject));

        assertThat(subject.linkTx()).isEqualTo(123456L);
    }

    @Test
    void shouldProcessFailIfReferenceIsNotFound() throws Exception {
        //given
        when(message.txReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(null);

        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Tx not found for tx reference: " + "TxReference" + " and accountId: 0");
    }

    @Test
    void shouldProcessFailForActivatedJournelType() throws Exception {
        //given
        when(message.txReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Activated.toString());

        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        EventAlreadyProcessedException exception = assertThrows(EventAlreadyProcessedException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Processing 'Activate' message. Tx 0 is in status Activated (journalId: 0). Skipping the message without processing.");
    }


    @Test
    void shouldProcessFailForAnyOtherThanCreatedJournalType() throws Exception {
        //given
        when(message.txReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Cancelled.toString());

        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).isEqualTo("Processing 'Activate' message. Tx 0 is in status 'Cancelled' cannot be activated.");
    }

    @Test
    void shouldProcessFailForInvalidAmount() throws Exception {
        //given
        when(message.txReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Created.toString());
        when(txStatus.getAmount()).thenReturn(Amount.of(456, 12));

        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).contains("The amount specified in the message amount does not match with the amount in the original tx");
    }

    @Test
    void shouldProcessFailIfIssuerAccountNotFound() throws Exception {
        //given
        when(message.txReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Created.toString());
        when(txStatus.getAmount()).thenReturn(amount);

        JsonObject additionalData = new Gson().fromJson("{ \"issuer\" : \"issuer\"}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(null);


        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );


        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, subject::process);

        //then
        assertThat(exception.getMessage()).contains("'issuer' issuer not found.");
    }

    @Test
    void shouldNotCreateNewTx() throws Exception {
        //given
        when(message.txReference()).thenReturn("TxReference");
        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );

        //when & then
        assertThat(subject.addNewTx()).isFalse();
    }

    @Test
    void shouldUpdateSpecializedTx() throws Exception {
        //given
        when(message.txReference()).thenReturn("TxReference");

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Created.toString());
        when(txStatus.getAmount()).thenReturn(amount);
        when(txStatus.getTxId()).thenReturn(123456L);

        JsonObject additionalData = new Gson().fromJson("{ " +
                "\"issuer\" : \"issuer\"," +
                "\"expiryDate\" : \"2023-12-15\"," +
                "\"activatingLocationRef\" : \"POS-LOC2\"," +
                "\"activationTimezone\" : \"Europe/Warsaw\"," +
                "\"activationTimestamp\" : \"2021-12-15T11:38:32Z\"" +
                "}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(issuerAccount);

        OffsetDateTime expectedActivationTimestamp = OffsetDateTime.ofInstant(Instant.parse("2021-12-15T11:38:32Z"), ZoneId.of("Europe/Warsaw"));


        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );
        subject.process();


        //when
        subject.updateSpecializedTx();

        //then
//        verify(repositoryHelper.getJournalDAO()).updateTokenTx(
//                eq(txStatus.getTxId()),
//                isNull(),
//                eq("POS-LOC2"),
//                eq(LocalDate.of(2023, 12, 15)),
//                eq(expectedActivationTimestamp),
//                eq("Europe/Warsaw"),
//                isNull()
//        );
    }


    @Test
    void shouldBuildJournalLines() throws Exception {
        //given
        final long journalId = 8921738921L;
        final int journalTypeId = 892;
        final long accountId = 756863L;
        final long issuerAccountId = 7807433L;
        final long fee1AccountId = 39732L;
        final long fee2AccountId = 4356L;
        final int txTypeId = 342;
        final int currencyId = 342;

        final int registerTypeTokenCreatedId = 564654;
        final int registerTypeTokenActivatedId = 7357832;
        final int registerTypeOpenBalanceId = 657674;
        final int registerTypeTokenReceivableId = 865456;
        final int registerTypeIssuerPrefundAccountId = 89124;
        final int registerTypeCardsPayablePendingId = 5234;
        final int registerTypeFeeId = 52324;

        final long registerTypeTokenCreatedBatchId = registerTypeTokenCreatedId * 100;
        final long registerTypeTokenActivatedBatchId = registerTypeTokenActivatedId * 100;
        final long registerTypeOpenBalanceBatchId = registerTypeOpenBalanceId * 100;
        final long registerTypeTokenReceivableBatchId = registerTypeTokenReceivableId * 100;
        final long registerTypeIssuerPrefundAccountBatchId = registerTypeIssuerPrefundAccountId * 100;
        final long registerTypeCardsPayablePendingBatchId = registerTypeCardsPayablePendingId * 100;
        final long registerTypeFeeBatch1Id = registerTypeFeeId * 100;
        final long registerTypeFeeBatch2Id = registerTypeFeeId * 100 + 10;

        when(message.txReference()).thenReturn("TxReference");
        when(journalType.getJournalTypeId()).thenReturn(journalTypeId);
        when(txType.getTxTypeId()).thenReturn(txTypeId);

        when(amount.getQuantity()).thenReturn(1000L);
        when(amount.getCurrencyId()).thenReturn(currencyId);


        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Created.toString());
        when(txStatus.getAmount()).thenReturn(amount);

        JsonObject additionalData = new Gson().fromJson("{ " +
                "\"issuer\" : \"issuer\"," +
                "\"expiryDate\" : \"2023-12-15\"," +
                "\"activatingLocationRef\" : \"POS-LOC2\"," +
                "\"activationTimezone\" : \"Europe/Warsaw\"," +
                "\"activationTimestamp\" : \"2021-12-15T11:38:32Z\"" +
                "}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(issuerAccount);

        when(account.getAccountId()).thenReturn(accountId);
        when(issuerAccount.getAccountId()).thenReturn(issuerAccountId);

        when(repositoryHelper.getRegisterTypeRepo().get("TokenCreated")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenCreatedId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("TokenActivated")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenActivatedId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("OpenBalance")).thenReturn(RegisterType.builder().registerTypeId(registerTypeOpenBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("TokenReceivable")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenReceivableId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("IssuerPrefundAccount")).thenReturn(RegisterType.builder().registerTypeId(registerTypeIssuerPrefundAccountId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("CardsPayablePending")).thenReturn(RegisterType.builder().registerTypeId(registerTypeCardsPayablePendingId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("Fee")).thenReturn(RegisterType.builder().registerTypeId(registerTypeFeeId).build());

        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenCreatedId, accountId)).thenReturn(registerTypeTokenCreatedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenActivatedId, accountId)).thenReturn(registerTypeTokenActivatedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId)).thenReturn(registerTypeOpenBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenReceivableId, accountId)).thenReturn(registerTypeTokenReceivableBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeIssuerPrefundAccountId, issuerAccountId)).thenReturn(registerTypeIssuerPrefundAccountBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId)).thenReturn(registerTypeCardsPayablePendingBatchId);

        when(journalMaker.getOrCreateAvailableBatch(registerTypeFeeId, fee1AccountId)).thenReturn(registerTypeFeeBatch1Id);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeFeeId, fee2AccountId)).thenReturn(registerTypeFeeBatch2Id);

        //add 3 fees, which only one (second) is valid for this account and txType
        List<FeeBase> fees = asList(
                buildFeeBase(accountId, 1),
                buildFeeBase(accountId, txTypeId),
                buildFeeBase(1, 1)
        );
        //add just one tier, detailed test of AbstractTemplate.getFeeTier() are in AbstractTemplateTest
        fees.get(1).setFeeTiers(List.of(
                new FeeTier(1265, 234523, "EXACT", 1000, asList(
                        new FeeTierDetail(44001, 1265, 3400L, 30, (int) fee1AccountId),
                        new FeeTierDetail(44002, 1265, 1400L, 10, (int) fee2AccountId)
                ))
        ));
        when(repositoryHelper.getFeeBaseRepo().getByJournalTypeId(anyInt())).thenReturn(fees);

        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeTokenCreatedBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenActivatedBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeFeeBatch1Id).quantity(3400).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeFeeBatch2Id).quantity(1400).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeOpenBalanceBatchId).quantity(-1000-3400-1400).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenReceivableBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeIssuerPrefundAccountBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeCardsPayablePendingBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build()
                );


        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );
        subject.process();


        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenCreatedId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenActivatedId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenReceivableId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeIssuerPrefundAccountId, issuerAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeFeeId, fee1AccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeFeeId, fee2AccountId);

        assertThat(result).isEqualTo(expectedJournalLines);

    }

    @Test
    void shouldBuildJournalLinesNoFees() throws Exception {
        //given
        final long journalId = 8921738921L;
        final int journalTypeId = 892;
        final long accountId = 756863L;
        final long issuerAccountId = 7807433L;
        final int currencyId = 342;

        final int registerTypeTokenCreatedId = 564654;
        final int registerTypeTokenActivatedId = 7357832;
        final int registerTypeOpenBalanceId = 657674;
        final int registerTypeTokenReceivableId = 865456;
        final int registerTypeIssuerPrefundAccountId = 89124;
        final int registerTypeCardsPayablePendingId = 5234;

        final long registerTypeTokenCreatedBatchId = registerTypeTokenCreatedId * 100;
        final long registerTypeTokenActivatedBatchId = registerTypeTokenActivatedId * 100;
        final long registerTypeOpenBalanceBatchId = registerTypeOpenBalanceId * 100;
        final long registerTypeTokenReceivableBatchId = registerTypeTokenReceivableId * 100;
        final long registerTypeIssuerPrefundAccountBatchId = registerTypeIssuerPrefundAccountId * 100;
        final long registerTypeCardsPayablePendingBatchId = registerTypeCardsPayablePendingId * 100;

        when(message.txReference()).thenReturn("TxReference");
        when(journalType.getJournalTypeId()).thenReturn(journalTypeId);

        when(amount.getQuantity()).thenReturn(1000L);
        when(amount.getCurrencyId()).thenReturn(currencyId);

        when(repositoryHelper.getJournalDAO().getTxStatusByReferenceAndAccountId(anyString(), anyLong())).thenReturn(txStatus);
        when(txStatus.getJournalTypeCode()).thenReturn(Constants.JournalType.Created.toString());
        when(txStatus.getAmount()).thenReturn(amount);

        JsonObject additionalData = new Gson().fromJson("{ " +
                "\"issuer\" : \"issuer\"," +
                "\"expiryDate\" : \"2023-12-15\"," +
                "\"activatingLocationRef\" : \"POS-LOC2\"," +
                "\"activationTimezone\" : \"Europe/Warsaw\"," +
                "\"activationTimestamp\" : \"2021-12-15T11:38:32Z\"" +
                "}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(repositoryHelper.getAccountRepo().getByCodeAndType(any(), anyString())).thenReturn(issuerAccount);

        when(account.getAccountId()).thenReturn(accountId);
        when(issuerAccount.getAccountId()).thenReturn(issuerAccountId);

        when(repositoryHelper.getRegisterTypeRepo().get("TokenCreated")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenCreatedId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("TokenActivated")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenActivatedId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("OpenBalance")).thenReturn(RegisterType.builder().registerTypeId(registerTypeOpenBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("TokenReceivable")).thenReturn(RegisterType.builder().registerTypeId(registerTypeTokenReceivableId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("IssuerPrefundAccount")).thenReturn(RegisterType.builder().registerTypeId(registerTypeIssuerPrefundAccountId).build());
        when(repositoryHelper.getRegisterTypeRepo().get("CardsPayablePending")).thenReturn(RegisterType.builder().registerTypeId(registerTypeCardsPayablePendingId).build());

        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenCreatedId, accountId)).thenReturn(registerTypeTokenCreatedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenActivatedId, accountId)).thenReturn(registerTypeTokenActivatedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId)).thenReturn(registerTypeOpenBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeTokenReceivableId, accountId)).thenReturn(registerTypeTokenReceivableBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeIssuerPrefundAccountId, issuerAccountId)).thenReturn(registerTypeIssuerPrefundAccountBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId)).thenReturn(registerTypeCardsPayablePendingBatchId);

        when(repositoryHelper.getFeeBaseRepo().getByJournalTypeId(anyInt())).thenReturn(null);

        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeTokenCreatedBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenActivatedBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeOpenBalanceBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeTokenReceivableBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeIssuerPrefundAccountBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeCardsPayablePendingBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build()
        );

        TokenActivate subject = new TokenActivate(
                journalMaker, account, amount,
                journalType, txType, message, repositoryHelper
        );
        subject.process();

        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenCreatedId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenActivatedId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeTokenReceivableId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeIssuerPrefundAccountId, issuerAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeCardsPayablePendingId, issuerAccountId);

        assertThat(result).isEqualTo(expectedJournalLines);
    }

    private FeeBase buildFeeBase(long accountId, long txTypeId) {
        return FeeBase.builder()
                .accountId((int) accountId)
                .txTypeId((int) txTypeId)
                .build();
    }

    @Test
    void shouldUseAdditionalDataBookingDate() throws Exception {
        //given
        String bookingDateAsString = "2022-05-15T11:38:32Z";

        additionalData = new Gson().fromJson("{ " +
                "\"publishableId\" : \"4587458\"," +
                "\"bookingDate\" : \"" + bookingDateAsString + "\"," +
                "\"activatingLocationRef\" : \"POS-LOC2\"" +
                "}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(message.txReference()).thenReturn("txRef1");
        TxVariant txv = new TxVariant();
        txv.setTxVariantCode("BASE");
        txv.setTxVariantId(1);
        when(repositoryHelper.getTxVariantRepo().get(anyString())).thenReturn(txv);

        TokenActivate subject = new TokenActivate(
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
                "\"publishableId\" : \"4587458\"," +
                "\"bookingDate\" : \"2028-12-30T11:00:00.00Z\"," +
                "\"activatingLocationRef\" : \"POS-LOC2\"" +
                "}", JsonObject.class);
        when(message.additionalData()).thenReturn(additionalData);
        when(message.txReference()).thenReturn("txRef1");

        // when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class,
                () -> new TokenActivate(journalMaker, account, amount, journalType, txType, message, repositoryHelper));

        //then
        assertThat(exception.getMessage()).contains("Invalid message.additionalData: the 'bookingDate' cannot be in the future");
    }

}