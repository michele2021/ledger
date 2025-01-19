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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.accounting.commons.Message.AdditionalData.bookingDate;
import static com.accounting.commons.Message.AdditionalData.description;
import static java.util.Objects.isNull;

public class AccountAddFunds extends AbstractTemplate {

    private Amount txAmount;
    private Amount handlingFeeAmount;
    private Amount discountAmount;
    private Amount vatAmount;
    private Amount prefundGLAmount;
    private final String bankAccountCode;
    private final int currencyId;
    private final String currencyCode;
    final OffsetDateTime bookingDateTime;

    public AccountAddFunds(JournalMaker journalMaker,
                           Account account,
                           Amount amount,
                           JournalType journalType,
                           TxType txType,
                           QueueMessage message,
                           RepositoryHelper repositoryHelper) throws AccountingMakerException {
        super(txType, account, message.getUuid(), journalType, journalMaker, amount, message.getAdditionalData(), repositoryHelper);
        throwExceptionIf(amount.getQuantity() <= 0, "The amount should be greater than zero, but was " + Constants.fromMU(amount));
        bankAccountCode = getAdditionalData(Message.AdditionalData.bankAccountCode).getAsString();
        currencyId = amount.getCurrencyId();
        currencyCode = amount.getCurrencyCode();
        bookingDateTime = getAdditionalData(bookingDate, this::toOffsetDateTime);
        validateBookingDateInThePast(bookingDateTime);
        prepareAmounts();
    }

    @Override
    public OffsetDateTime getJournalBookingDate() {
        return bookingDateTime;
    }

    @Override
    public Tx buildTx() {
        Tx tx = buildBaseTx();

        // txReference
        if (isNull(this.txReference)) {
            String uuid = UUID.randomUUID().toString();
            String txRef = txType.getCode().substring(0, 3).toUpperCase().concat("_").concat(uuid);
            tx.setTxReference(txRef);
        }

        // txDescription
        String txDescription = getAdditionalData(description, JsonElement::getAsString);
        String desc = txDescription == null ? null : txDescription.substring(0, Math.min(txDescription.length(), 255));
        tx.setDescription(desc);

        // amount
        tx.setAmount(txAmount);

        return tx;
    }

    // N.B. Achtung: when changing "buildJournalLines", also the method "buildJournalLinesCorrection"
    // might possibly need the same change.
    @Override
    public List<JournalLine> buildJournalLines(long journalId) throws AccountingMakerException {
        List<JournalLine> journalLines = new ArrayList<>();

        long accountId = account.getAccountId();
        Account bankAccount = accountRepo.getByCodeAndType(bankAccountCode, Constants.AccountType.BankAccount.toString());
        if(bankAccount == null) {
            bankAccount = accountRepo.getByCodeAndType(bankAccountCode, Constants.AccountType.VirtualIban.toString());
        }
        throwExceptionIfNull(bankAccount, "Bank Account not found for account code: " + bankAccountCode);
        assert bankAccount != null;
        long bankAccountId = bankAccount.getAccountId();

        String ccAccountCode = Constants.BusinessEntityAccount.CleverCards.toString();
        Account ccAccount = accountRepo.getByCodeAndType(ccAccountCode, Constants.AccountType.BusinessEntity.toString());
        throwExceptionIfNull(ccAccount, "Business Entity not found for account code: " + ccAccount);
        assert ccAccount != null;
        long ccAccountId = ccAccount.getAccountId();

        // N.B. when changing these journal lines, we also need to change accordingly buildJournalLinesCorrection (reversed journal)
        // Debit/Credit
        addDebitLine(journalId, journalLines, Constants.RegisterType.BankBalance, bankAccountId, txAmount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.BankReceived, bankAccountId, txAmount);
        // Handling Fee + VAT
        if (Objects.nonNull(handlingFeeAmount)) {
            addDebitLine(journalId, journalLines, Constants.RegisterType.Fee, ccAccountId, handlingFeeAmount);
            addDebitLine(journalId, journalLines, Constants.RegisterType.VAT, ccAccountId, vatAmount);
        }
        // Discount
//        if (Objects.nonNull(discountAmount)) {
//            addDebitLine(journalId, journalLines, Constants.RegisterType.ClientDiscount, accountId, discountAmount);
//            addCreditLine(journalId, journalLines, Constants.RegisterType.ClientDiscountPayable, ccAccountId, discountAmount);
//        }
        // Debit/Credit
        addDebitLine(journalId, journalLines, Constants.RegisterType.OpenBalance, accountId, amount);
        addCreditLine(journalId, journalLines, Constants.RegisterType.PrefundGL, accountId, prefundGLAmount);

        return journalLines;
    }

    // This builds a "reversed" journal
    public List<JournalLine> buildJournalLinesCorrection(long journalId) throws AccountingMakerException {
        List<JournalLine> journalLines = new ArrayList<>();

        long accountId = account.getAccountId();
        Account bankAccount = accountRepo.getByCodeAndType(bankAccountCode, Constants.AccountType.BankAccount.toString());
        throwExceptionIfNull(bankAccount, "Bank Account not found for account code: " + bankAccountCode);
        assert bankAccount != null;
        long bankAccountId = bankAccount.getAccountId();

        String ccAccountCode = Constants.BusinessEntityAccount.CleverCards.toString();
        Account ccAccount = accountRepo.getByCodeAndType(ccAccountCode, Constants.AccountType.BusinessEntity.toString());
        throwExceptionIfNull(ccAccount, "Business Entity not found for account code: " + ccAccount);
        assert ccAccount != null;
        long ccAccountId = ccAccount.getAccountId();

        // Debit/Credit
        addCreditLine(journalId, journalLines, Constants.RegisterType.BankBalance, bankAccountId, txAmount);
        addDebitLine(journalId, journalLines, Constants.RegisterType.BankReceived, bankAccountId, txAmount);
        // Handling Fee + VAT
        if (Objects.nonNull(handlingFeeAmount)) {
            addCreditLine(journalId, journalLines, Constants.RegisterType.Fee, ccAccountId, handlingFeeAmount);
            addCreditLine(journalId, journalLines, Constants.RegisterType.VAT, ccAccountId, vatAmount);
        }
        // Discount
//        if (Objects.nonNull(discountAmount)) {
//            addCreditLine(journalId, journalLines, Constants.RegisterType.ClientDiscount, accountId, discountAmount);
//            addDebitLine(journalId, journalLines, Constants.RegisterType.ClientDiscountPayable, ccAccountId, discountAmount);
//        }
        // Debit/Credit
        addCreditLine(journalId, journalLines, Constants.RegisterType.OpenBalance, accountId, amount);
        addDebitLine(journalId, journalLines, Constants.RegisterType.PrefundGL, accountId, prefundGLAmount);

        return journalLines;
    }

    private void prepareAmounts() throws AccountingMakerException {
        BigDecimal handlingFeeAmountBD = getAdditionalData(Message.AdditionalData.handlingFeeAmount, JsonElement::getAsBigDecimal);
        BigDecimal vatAmountBD = getAdditionalData(Message.AdditionalData.vatAmount, JsonElement::getAsBigDecimal);
        BigDecimal discountAmountBD = getAdditionalData(Message.AdditionalData.discountAmount, JsonElement::getAsBigDecimal);

        txAmount = prefundGLAmount = amount;
        if(Objects.nonNull(discountAmountBD)) {
            throwExceptionIf(Objects.nonNull(handlingFeeAmountBD), "Handling Fee cannot be specified when a discount is present.");
            throwExceptionIf(Objects.nonNull(vatAmountBD), "VAT cannot be specified when a discount is present.");
            throwExceptionIf((discountAmountBD.compareTo(BigDecimal.ZERO) <= 0), "Discount amount should be greater than zero, but was " + discountAmountBD);
            discountAmount = Amount.of(Constants.toMinorUnits(currencyCode, discountAmountBD), currencyId);
            long txQuantity = amount.getQuantity() - discountAmount.getQuantity();
            txAmount = Amount.of(txQuantity, currencyId);
            prefundGLAmount = amount;
        }
        if(Objects.nonNull(handlingFeeAmountBD) || Objects.nonNull(vatAmountBD)) {
            throwExceptionIfNull(vatAmountBD, "VAT amount needs to be specified when a Handling Fee is present.");
            throwExceptionIfNull(handlingFeeAmountBD, "Handling Fee amount needs to be specified when VAT is present.");
            assert vatAmountBD != null;
            assert handlingFeeAmountBD != null;
            throwExceptionIf(vatAmountBD.compareTo(BigDecimal.ZERO) < 0, "VAT amount should be greater or equal to zero, but was " + vatAmountBD);
            throwExceptionIf(handlingFeeAmountBD.compareTo(BigDecimal.ZERO) <= 0, "Handling Fee amount should be greater than zero, but was " + handlingFeeAmountBD);
            handlingFeeAmount = Amount.of(Constants.toMinorUnits(currencyCode, handlingFeeAmountBD), currencyId);
            vatAmount = Amount.of(Constants.toMinorUnits(currencyCode, vatAmountBD), currencyId);
            long txQuantity = amount.getQuantity() + handlingFeeAmount.getQuantity() + vatAmount.getQuantity();
            txAmount = Amount.of(txQuantity, currencyId);
            prefundGLAmount = txAmount;
        }
    }
}
