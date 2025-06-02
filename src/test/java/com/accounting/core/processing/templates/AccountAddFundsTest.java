package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.entities.*;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.queue.QueueMessage;
import com.accounting.core.entities.JournalLine;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.accounting.core.testUtils.TestUtils.setUpMockRepositoryHelper;
import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountAddFundsTest {

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
    Account bankAccount;

    @Mock
    Account ccAccount;

    RepositoryHelper repositoryHelper = setUpMockRepositoryHelper();

    final int currencyId = 11;
    final String currencyCode = Constants.Currencies.EUR.toString();

    @Test
    void shouldConstructorValidateAmountGreaterThanZero() {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\" }", JsonObject.class);

        when(amount.getQuantity()).thenReturn(-300L);
        when(amount.getCurrencyCode()).thenReturn(currencyCode);
        when(message.additionalData()).thenReturn(additionalData);

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, () ->
                new AccountAddFunds(journalMaker, account, amount, journalType, txType, message, repositoryHelper));

        //then
        assertThat(exception.getMessage()).isEqualTo("The amount should be greater than zero, but was -3.00");
    }

    @Test
    void shouldConstructorValidateAdditionalDataHandlingFeeNoVat() {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\", \"handlingFeeAmount\" : 10.2 }", JsonObject.class);
        shouldConstructorThrowException(additionalData, "VAT amount needs to be specified when a Handling Fee is present.");
    }

    @Test
    void shouldConstructorValidateAdditionalDataVatNoHandlingFee() {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\", \"vatAmount\" : 10.2 }", JsonObject.class);
        shouldConstructorThrowException(additionalData, "Handling Fee amount needs to be specified when VAT is present.");
    }

    @Test
    void shouldConstructorValidateAdditionalDataVatHandlingFeeDiscount() {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\", \"vatAmount\" : 10.2, \"handlingFeeAmount\" : 10.2, \"discountAmount\" : 10.2 }", JsonObject.class);
        shouldConstructorThrowException(additionalData, "Handling Fee cannot be specified when a discount is present.");
    }

    private void shouldConstructorThrowException(JsonObject additionalData, String msg) {
        //given
        when(amount.getQuantity()).thenReturn(300L);
        when(amount.getCurrencyCode()).thenReturn(currencyCode);
        when(message.additionalData()).thenReturn(additionalData);

        //when
        AccountingMakerException exception = assertThrows(AccountingMakerException.class, () ->
                new AccountAddFunds(journalMaker, account, amount, journalType, txType, message, repositoryHelper));

        //then
        assertThat(exception.getMessage()).isEqualTo(msg);
    }

    @Test
    void shouldProcessOK() throws Exception {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\" }", JsonObject.class);
        shouldProcessOkInternal(additionalData);
    }

    @Test
    void shouldProcessOKWithHandlingFeeAndVat() throws Exception {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\", \"handlingFeeAmount\" : 10.2, \"vatAmount\" : 2 }", JsonObject.class);
        shouldProcessOkInternal(additionalData);
    }

    @Test
    void shouldProcessOKWithDiscount() throws Exception {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\", \"discountAmount\" : 10.2 }", JsonObject.class);
        shouldProcessOkInternal(additionalData);
    }

    private void shouldProcessOkInternal(JsonObject additionalData) throws AccountingMakerException {
        //given
        when(message.additionalData()).thenReturn(additionalData);
        when(amount.getCurrencyCode()).thenReturn(currencyCode);
        when(amount.getCurrencyId()).thenReturn(currencyId);
        when(amount.getQuantity()).thenReturn(1000L);

        AccountAddFunds subject = new AccountAddFunds(journalMaker, account, amount, journalType, txType, message, repositoryHelper);

        //when
        subject.process();

        //then
        verify(journalMaker).createJournal(same(subject));
    }


    @Test
    void shouldCreateNewTx() throws Exception {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\" }", JsonObject.class);

        //given
        when(message.additionalData()).thenReturn(additionalData);
        when(amount.getCurrencyCode()).thenReturn(currencyCode);
        when(amount.getCurrencyId()).thenReturn(currencyId);
        when(amount.getQuantity()).thenReturn(1000L);

        AccountAddFunds subject = new AccountAddFunds(journalMaker, account, amount, journalType, txType, message, repositoryHelper);

        //when & then
        assertThat(subject.addNewTx()).isTrue();
    }


    @Test
    void shouldBuildJournalLines() throws Exception {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\" }", JsonObject.class);

        //given
        final long journalId = 8921738921L;
        final long accountId = account.getAccountId();
        final long bankAccountId = bankAccount.getAccountId();
        final int currencyId = 342;

        final int registerTypeBankBalanceId = 5471;
        final int registerTypeBankReceivedId = 5234;
        final int registerTypeOpenBalanceId = 7357832;
        final int registerTypePrefundGLId = 5345;

        final long registerTypeBankBalanceBatchId = registerTypeBankBalanceId * 100;
        final long registerTypeBankReceivedBatchId = registerTypeBankReceivedId * 100;
        final long registerTypeOpenBalanceBatchId = registerTypeOpenBalanceId * 100;
        final long registerTypePrefundGLBatchId = registerTypePrefundGLId * 100;

        when(message.additionalData()).thenReturn(additionalData);
        when(amount.getCurrencyCode()).thenReturn(currencyCode);
        when(amount.getCurrencyId()).thenReturn(currencyId);
        when(amount.getQuantity()).thenReturn(1000L);

        when(repositoryHelper.getAccountRepo().getByCodeAndType("ItalyClients1", Constants.AccountType.BankAccount.toString()))
                .thenReturn(bankAccount);
        when(repositoryHelper.getAccountRepo().getByCodeAndType("r30", Constants.AccountType.Company.toString()))
                .thenReturn(ccAccount);

        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.BankBalance.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeBankBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.BankReceived.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeBankReceivedId).build());
        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.Clearing.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeOpenBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.Receivable.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypePrefundGLId).build());

        when(journalMaker.getOrCreateAvailableBatch(registerTypeBankBalanceId, bankAccountId))
                .thenReturn(registerTypeBankBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeBankReceivedId, bankAccountId))
                .thenReturn(registerTypeBankReceivedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId))
                .thenReturn(registerTypeOpenBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypePrefundGLId, accountId)).
                thenReturn(registerTypePrefundGLBatchId);

        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeBankBalanceBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeBankReceivedBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeOpenBalanceBatchId).quantity(1000).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypePrefundGLBatchId).quantity(-1000).journalId(journalId).currencyId(currencyId).build());

        AccountAddFunds subject = new AccountAddFunds(journalMaker, account, amount, journalType, txType, message, repositoryHelper);
        subject.process();

        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeBankBalanceId, bankAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeBankReceivedId, bankAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypePrefundGLId, accountId);

        AssertionsForInterfaceTypes.assertThat(result).isEqualTo(expectedJournalLines);
    }

    @Test
    void shouldBuildJournalLinesWithHandlingFeeAndVat() throws Exception {
        final long qty = 100000L;
        final long fee = 10000L;
        final long vat = 2200L;
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\", \"handlingFeeAmount\" : 100, \"vatAmount\" : 22 }", JsonObject.class);
        jlWithHandlingFeeAndVatInternal(additionalData, qty, fee, vat);
    }

    // VAT exempt (we create a journal line with quantity 0)
    @Test
    void shouldBuildJournalLinesWithHandlingFeeAndZeroVat() throws Exception {
        final long qty = 100000L;
        final long fee = 10000L;
        final long vat = 0L;
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\", \"handlingFeeAmount\" : 100, \"vatAmount\" : 0 }", JsonObject.class);
        jlWithHandlingFeeAndVatInternal(additionalData, qty, fee, vat);
    }

    private void jlWithHandlingFeeAndVatInternal(JsonObject additionalData, long qty, long fee, long vat) throws AccountingMakerException {
        //given
        final long journalId = 8921738921L;
        final long accountId = account.getAccountId();
        final long bankAccountId = bankAccount.getAccountId();
        final long ccAccountId = ccAccount.getAccountId();
        final int currencyId = 342;

        final int registerTypeBankBalanceId = 5471;
        final int registerTypeBankReceivedId = 5234;
        final int registerTypeOpenBalanceId = 7332;
        final int registerTypePrefundGLId = 5345;
        final int registerTypeFeeId = 5347;
        final int registerTypeVATId = 5346;

        final long registerTypeBankBalanceBatchId = registerTypeBankBalanceId * 100;
        final long registerTypeBankReceivedBatchId = registerTypeBankReceivedId * 100;
        final long registerTypeOpenBalanceBatchId = registerTypeOpenBalanceId * 100;
        final long registerTypePrefundGLBatchId = registerTypePrefundGLId * 100;
        final long registerTypeFeeBatchId = registerTypeFeeId * 100;
        final long registerTypeVATBatchId = registerTypeVATId * 100;

        when(message.additionalData()).thenReturn(additionalData);
        when(amount.getCurrencyCode()).thenReturn(currencyCode);
        when(amount.getCurrencyId()).thenReturn(currencyId);
        when(amount.getQuantity()).thenReturn(qty);

        when(repositoryHelper.getAccountRepo().getByCodeAndType("ItalyClients1", Constants.AccountType.BankAccount.toString()))
                .thenReturn(bankAccount);
        when(repositoryHelper.getAccountRepo().getByCodeAndType("r30", Constants.AccountType.Company.toString()))
                .thenReturn(ccAccount);

        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.BankBalance.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeBankBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.BankReceived.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeBankReceivedId).build());

        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.Fee.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeFeeId).build());
        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.VAT_OUT.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeVATId).build());

        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.Clearing.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeOpenBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.Receivable.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypePrefundGLId).build());

        when(journalMaker.getOrCreateAvailableBatch(registerTypeBankBalanceId, bankAccountId))
                .thenReturn(registerTypeBankBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeBankReceivedId, bankAccountId))
                .thenReturn(registerTypeBankReceivedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeFeeId, ccAccountId))
                .thenReturn(registerTypeFeeBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeVATId, ccAccountId))
                .thenReturn(registerTypeVATBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId))
                .thenReturn(registerTypeOpenBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypePrefundGLId, accountId)).
                thenReturn(registerTypePrefundGLBatchId);

        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeBankBalanceBatchId).quantity(qty+fee+vat).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeBankReceivedBatchId).quantity(-(qty+fee+vat)).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeFeeBatchId).quantity(fee).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeVATBatchId).quantity(vat).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeOpenBalanceBatchId).quantity(qty).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypePrefundGLBatchId).quantity(-(qty+fee+vat)).journalId(journalId).currencyId(currencyId).build());

        AccountAddFunds subject = new AccountAddFunds(journalMaker, account, amount, journalType, txType, message, repositoryHelper);
        subject.process();

        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeBankBalanceId, bankAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeBankReceivedId, bankAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeFeeId, ccAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeVATId, ccAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypePrefundGLId, accountId);

        AssertionsForInterfaceTypes.assertThat(result).isEqualTo(expectedJournalLines);
    }

    @Test
    void shouldBuildJournalLinesWithClientDiscount() throws Exception {
        JsonObject additionalData = new Gson().fromJson("{ \"bankAccountCode\" : \"ItalyClients1\", \"discountAmount\" : 100 }", JsonObject.class);

        //given
        final long journalId = 8921738921L;
        final long accountId = account.getAccountId();
        final long bankAccountId = bankAccount.getAccountId();
        final long ccAccountId = ccAccount.getAccountId();
        final int currencyId = 342;

        final int registerTypeBankBalanceId = 5471;
        final int registerTypeBankReceivedId = 5234;
        final int registerTypeOpenBalanceId = 7332;
        final int registerTypePrefundGLId = 5345;
        final int registerTypeClientDiscountId = 5347;
        final int registerTypeClientDiscountPayableId = 5348;

        final long registerTypeBankBalanceBatchId = registerTypeBankBalanceId * 100;
        final long registerTypeBankReceivedBatchId = registerTypeBankReceivedId * 100;
        final long registerTypeOpenBalanceBatchId = registerTypeOpenBalanceId * 100;
        final long registerTypePrefundGLBatchId = registerTypePrefundGLId * 100;
        final long registerTypeClientDiscountBatchId = registerTypeClientDiscountId * 100;
        final long registerTypeClientDiscountPayableBatchId = registerTypeClientDiscountPayableId * 100;

        final long qty = 100000L;
        final long discount = 10000L;

        when(message.additionalData()).thenReturn(additionalData);
        when(amount.getCurrencyCode()).thenReturn(currencyCode);
        when(amount.getCurrencyId()).thenReturn(currencyId);
        when(amount.getQuantity()).thenReturn(qty);

        when(repositoryHelper.getAccountRepo().getByCodeAndType("ItalyClients1", Constants.AccountType.BankAccount.toString()))
                .thenReturn(bankAccount);
        when(repositoryHelper.getAccountRepo().getByCodeAndType("r30", Constants.AccountType.Company.toString()))
                .thenReturn(ccAccount);

        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.BankBalance.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeBankBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.BankReceived.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeBankReceivedId).build());

        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.Clearing.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypeOpenBalanceId).build());
        when(repositoryHelper.getRegisterTypeRepo().get(Constants.RegisterType.Receivable.toString()))
                .thenReturn(RegisterType.builder().registerTypeId(registerTypePrefundGLId).build());

        when(journalMaker.getOrCreateAvailableBatch(registerTypeBankBalanceId, bankAccountId))
                .thenReturn(registerTypeBankBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeBankReceivedId, bankAccountId))
                .thenReturn(registerTypeBankReceivedBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeClientDiscountId, ccAccountId))
                .thenReturn(registerTypeClientDiscountBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeClientDiscountPayableId, ccAccountId))
                .thenReturn(registerTypeClientDiscountPayableBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId))
                .thenReturn(registerTypeOpenBalanceBatchId);
        when(journalMaker.getOrCreateAvailableBatch(registerTypePrefundGLId, accountId)).
                thenReturn(registerTypePrefundGLBatchId);

        List<JournalLine> expectedJournalLines = asList(
                JournalLine.builder().batchId(registerTypeBankBalanceBatchId).quantity(qty-discount).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeBankReceivedBatchId).quantity(-(qty-discount)).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeClientDiscountBatchId).quantity(discount).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeClientDiscountPayableBatchId).quantity(-discount).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypeOpenBalanceBatchId).quantity(qty).journalId(journalId).currencyId(currencyId).build(),
                JournalLine.builder().batchId(registerTypePrefundGLBatchId).quantity(-qty).journalId(journalId).currencyId(currencyId).build());

        AccountAddFunds subject = new AccountAddFunds(journalMaker, account, amount, journalType, txType, message, repositoryHelper);
        subject.process();

        //when
        List<JournalLine> result = subject.buildJournalLines(journalId);

        //then
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeBankBalanceId, bankAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeBankReceivedId, bankAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeClientDiscountId, ccAccountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypeOpenBalanceId, accountId);
        verify(journalMaker).getOrCreateAvailableBatch(registerTypePrefundGLId, accountId);

        AssertionsForInterfaceTypes.assertThat(result).isEqualTo(expectedJournalLines);
    }
}