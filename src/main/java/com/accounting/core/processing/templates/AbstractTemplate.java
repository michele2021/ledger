package com.accounting.core.processing.templates;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.entities.Account;
import com.accounting.commons.entities.Amount;
import com.accounting.commons.entities.JournalType;
import com.accounting.commons.entities.TxType;
import com.accounting.commons.exceptions.AccountingMakerException;
import com.accounting.commons.repository.AccountRepoIf;
import com.accounting.commons.repository.FeeBaseRepoIf;
import com.accounting.commons.repository.TxVariantRepoIf;
import com.accounting.commons.utils.SerializationUtil;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.entities.Tx;
import com.accounting.core.exceptions.AccountingTemplateException;
import com.accounting.core.processing.JournalLinesMaker;
import com.accounting.core.processing.JournalMaker;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Objects.isNull;


@Slf4j
abstract class AbstractTemplate extends JournalLinesMaker {

    private final JournalType journalType;
    protected final String txReference;
    protected Amount amount;
    protected final TxType txType;
    protected final Account account;
    protected final JournalDAO journalDAO;
    protected final JsonObject additionalData;
    protected final RepositoryHelper repositoryHelper;
    protected final TxVariantRepoIf txVariantRepo;
    protected final AccountRepoIf accountRepo;
    protected final FeeBaseRepoIf feeBaseRepo;
    protected final Gson gson;

    protected AbstractTemplate(TxType txType, Account account, String txReference,
                               JournalType journalType, JournalMaker journalMaker, Amount amount,
                               JsonObject additionalData, RepositoryHelper repositoryHelper) {

        super(journalMaker, repositoryHelper.getRegisterTypeRepo());
        this.txType = txType;
        this.account = account;
        this.txReference = txReference;
        this.journalType = journalType;
        this.amount = amount;
        this.additionalData = additionalData;
        this.repositoryHelper = repositoryHelper;
        this.journalDAO = repositoryHelper.getJournalDAO();
        this.accountRepo = repositoryHelper.getAccountRepo();
        this.txVariantRepo = repositoryHelper.getTxVariantRepo();
        this.feeBaseRepo = repositoryHelper.getFeeBaseRepo();
        this.gson = SerializationUtil.getGson();
    }

    @Override
    public JournalType getJournalType() {
        return journalType;
    }

    final Tx buildBaseTx(Long parentTxId) {
        Tx tx = buildBaseTx();
        tx.setParentTxId(parentTxId);
        return tx;
    }

    @Override
    public boolean isTestAccount() {
        return account.isTest();
    }

    final Tx buildBaseTx() {
        int txVariantId = txVariantRepo.get(Constants.TxVariant.BASE.toString()).getTxVariantId();
        // if the bookingDate is specified in the message "additionalData" (data fix), then we use it also for the creationDate of the tx.
        OffsetDateTime creationDate = getJournalBookingDate() == null ? OffsetDateTime.now() : getJournalBookingDate();
        return Tx.builder()
                .txTypeId(txType.getTxTypeId())
                .accountId(account.getAccountId())
                .amount(amount)
                .txReference(txReference)
                .creationDate(creationDate)
                .txVariantId(txVariantId)
                .build();
    }

    final Tx getTxByReference() throws AccountingMakerException {
        Tx tx = journalDAO.getTxByReference(txReference);
        throwExceptionIfNull(tx, format("Tx not found for tx reference: %s", txReference));
        return tx;
    }

    final void validateIfTxReferencePresent() throws AccountingTemplateException {
        if (txReference == null) {
            throw new AccountingTemplateException("Mandatory original txReference not found in the queue message.");
        }
    }

    final void validateAmount(Tx tx) throws AccountingMakerException {
        throwExceptionIf(amount != null && !tx.getAmount().equals(amount),
                format("The amount specified in the message %s does not match with the amount in the original tx: %s", amount, tx.getAmount()));
    }

    final void validateAccount(Tx tx) throws AccountingMakerException {
        throwExceptionIf(tx.getAccountId() != account.getAccountId(),
                format("Account id of the original Tx %d is different from the current account id %d", tx.getAccountId(), account.getAccountId()));
    }

    final void validateBookingDateInThePast(OffsetDateTime bookingDateTime) throws AccountingMakerException {
        if (bookingDateTime != null && bookingDateTime.isAfter(OffsetDateTime.now())) {
            throw new AccountingMakerException("Invalid message.additionalData: the 'bookingDate' cannot be in the future: " + bookingDateTime);
        }
    }

    final OffsetDateTime toOffsetDateTime(JsonElement e) {
        return OffsetDateTime.parse(e.getAsString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    final void throwExceptionIfNull(Object value, String s) throws AccountingMakerException {
        if (isNull(value)) {
            throw new AccountingMakerException(s);
        }
    }

    final void throwExceptionIf(boolean condition, String s) throws AccountingMakerException {
        if (condition) {
            throw new AccountingMakerException(s);
        }
    }

    @NotNull
    final JsonElement getMandatoryAdditionalData(Message.AdditionalData additionalDataType) throws AccountingMakerException {
        var jsonElement = additionalData.get(additionalDataType.toString());
        if (jsonElement == null) {
            // Normally, this should not be possible to happen, since we have json-schema validation.
            throw new AccountingMakerException("The 'additionalData' element in the message is missing the required field " + additionalDataType);
        }
        return jsonElement;
    }

    @Nullable
    final <TYPE> TYPE getAdditionalData(Message.AdditionalData additionalDataType, Function<JsonElement, TYPE> converter) {
        var jsonElement = additionalData.get(additionalDataType.toString());
        if (isNull(jsonElement) || jsonElement.isJsonNull()) {
            return null;
        }
        return converter.apply(jsonElement);
    }

//    final FeeTier getFeeTier() throws AccountingMakerException {
//        List<FeeBase> fees = feeBaseRepo.getByJournalTypeId(journalType.getJournalTypeId());
//        if (ObjectUtils.isEmpty(fees)) {
//            // no fees to apply
//            log.debug("No fees found to apply for journalType: {} ", journalType.getCode());
//            return null;
//        }
//        log.info("Fetched fees from cache for journalType: {}, fees: {}", journalType.getJournalTypeId(), fees);
//
//        // filter by account and txType
//        fees = fees.stream()
//                .filter(e -> e.getAccountId() == account.getAccountId())
//                .filter(e -> e.getTxTypeId() == null || e.getTxTypeId() == txType.getTxTypeId())
//                .toList();
//        if (fees.isEmpty()) {
//            // no fees to apply
//            log.debug("No fees found to apply for journalType '{}', account '{}' and txType '{}' ", journalType.getCode(), account.getAccountCode(), txType.getCode());
//            return null;
//        }
//        if (fees.size() > 1) {
//            throw new AccountingMakerException("Could not uniquely identify the fee to apply. Applicable fees: " + fees);
//        }
//        FeeBase fee = fees.getFirst();
//        // N.B. a basic validation of the fee record fetched from the DB is already done at application startup.
//
//        if (ObjectUtils.isEmpty(fee.getFeeTiers())) {
//            throw new AccountingMakerException("Expected tier definition not found for fees with Id: " + fee.getFeeId());
//        }
//        FeeTier feeTier = findTier(fee.getFeeTiers(), amount.getQuantity());
//        if (feeTier == null) {
//            throw new AccountingMakerException("No tier matching the exact amount: " + amount + ". Fees with Id: " + fee.getFeeId());
//        }
//        List<FeeTierDetail> feeTierDetails = feeTier.getFeeTierDetails();
//        if (feeTierDetails.size() != 2) {
//            throw new AccountingMakerException("Expected 2 lines for the tier with id: " + feeTier.getTierId() + ", but was: " + feeTierDetails.size());
//        }
//        return feeTier;
//    }
//
//    private FeeTier findTier(@Nonnull List<FeeTier> tiers, long quantity) {
//        for (FeeTier tier : tiers) {
//            // TODO refactor and support "LIMIT" tiers
//            if ("EXACT".equals(tier.getTierType()) && tier.getTierQuantity() == quantity) {
//                log.debug("found tier with id: {} exact quantity: {}", tier.getTierId(), tier.getTierQuantity());
//                return tier;
//            }
//        }
//        return null;
//    }
}
