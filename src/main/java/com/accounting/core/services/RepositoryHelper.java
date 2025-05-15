package com.accounting.core.services;

import com.accounting.commons.Constants;
import com.accounting.commons.SystemConstants;
import com.accounting.commons.entities.AccountingEntity;
import com.accounting.commons.entities.FeeBase;
import com.accounting.commons.infra.cache.management.CacheActionDAO;
import com.accounting.commons.infra.jobs.JobsDAO;
import com.accounting.commons.repository.*;
import com.accounting.commons.repository.database.AccountDAO;
import com.accounting.commons.repository.database.PlatformRepoIf;
import com.accounting.core.db.AccountWriteDAO;
import com.accounting.core.db.JournalDAO;
import com.accounting.core.db.MismatchedTokensDAO;
import com.accounting.core.queue.SQSAccountingBalanceQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

@Slf4j
@Getter
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class RepositoryHelper {

    private final Jdbi jdbi;
    private final AccountRepoIf accountRepo;
    private final AccountTypeRepoIf accountTypeRepo;
    private final CurrencyRepoIf currencyRepo;
    private final JournalTypeRepoIf journalTypeRepo;
    private final RegisterTypeRepoIf registerTypeRepo;
    private final SubsystemRepoIf subsystemRepo;
    private final TxTypeRepoIf txTypeRepo;
    private final TxVariantRepoIf txVariantRepo;
    private final PlatformRepoIf platformRepo;
    private final FeeBaseRepoIf feeBaseRepo;
    private final UserRepoIf userRepoIf;
    private final AccountDAO accountDAO;
    private final AccountWriteDAO accountWriteDAO;
    private final CacheActionDAO cacheActionDAO;
    private final JournalDAO journalDAO;
    private final MismatchedTokensDAO mismatchedTokensDAO;
    private final JobsDAO jobsDAO;
    private final SQSAccountingBalanceQueue accountingBalanceQueue;

    private void loadCaches() {
        accountTypeRepo.init();
        currencyRepo.init();
        journalTypeRepo.init();
        registerTypeRepo.init();
        txTypeRepo.init();
        subsystemRepo.init();
        txVariantRepo.init();
        platformRepo.init();
        feeBaseRepo.init();
        userRepoIf.init();
    }

    // Checks that all the constants defined in Java are also present in the database.
    private void validateConstantTypes(String entityName, Repository<? extends AccountingEntity> repo, String[] types) {
        for (String type : types) {
            AccountingEntity entity = repo.get(type);
            if (entity == null) {
                log.error("Config validation: {} '{}' not found in database!", entityName, type);
                String message = format("Exception: %s '%s' not found in database!", entityName, type);
                throw new WebApplicationException(message);
            }
        }
        int size = repo.size();
        // if the repository is a cache (size >=0) additionally check that all the values in the cache are also in the constants.
        if (size != -1 && size != types.length) {
            String message = format("Exception: %s - values in cache: %s , while values in Constants are %s", entityName, size, types.length);
            log.error("Config validation: {} - values in cache: {}, while values in Constants are {}", entityName, size, types.length);
            throw new WebApplicationException(message);
        }
    }

    private void validateFeeBase() {
        List<FeeBase> fees = feeBaseRepo.getAll();
        for (FeeBase fee : fees) {
            if ((fee.getFeeQuantity() != null && fee.getCurrencyId() == null) || (fee.getFeeQuantity() == null && fee.getCurrencyId() != null)) {
                throw new WebApplicationException("Contractual fee definition is inconsistent: quantity and currency need to be either both null or both set. FeeId: " + fee.getFeeId());
            }
            if (fee.getFeeQuantity() != null && fee.getFeePercentage() != null) {
                throw new WebApplicationException("Contractual fee definition is inconsistent: percentage and amount cannot be both specified. FeeId: " + fee.getFeeId());
            }
        }
    }

    private void validateConstants() {
        validateConstantTypes("AccountType", accountTypeRepo, Stream.of(Constants.AccountType.values()).map(Enum::name).toArray(String[]::new));
        validateConstantTypes("JournalType", journalTypeRepo, Stream.of(Constants.JournalType.values()).map(Enum::name).toArray(String[]::new));
        validateConstantTypes("RegisterType", registerTypeRepo, Stream.of(Constants.RegisterType.values()).map(Enum::name).toArray(String[]::new));
        validateConstantTypes("TxType", txTypeRepo, Stream.of(Constants.TxType.values()).map(Enum::name).toArray(String[]::new));
        validateConstantTypes("Currency", currencyRepo, Stream.of(Constants.Currencies.values()).map(Enum::name).toArray(String[]::new));
        validateConstantTypes("TxVariant", txVariantRepo, Stream.of(Constants.TxVariant.values()).map(Enum::name).toArray(String[]::new));
        validateConstantTypes("Platform", platformRepo, Stream.of(Constants.Platforms.values()).map(Enum::name).toArray(String[]::new));
        validateFeeBase();
    }

    /*
     * Load caches (if used) and validates the config constants.
     * N.B. Application should not start if this fails.
     */
    public void initAccountingConfig() {
        loadCaches();
        validateConstants();
    }

    public void refreshAccountCaches(long accountId) {
        accountRepo.refreshKey(accountId);
        // publish a message in the database so that all the interested services can process it (at the moment that is the "ledger-data-service").
        cacheActionDAO.addAction(SystemConstants.Cache.ACCOUNT, accountId, SystemConstants.CacheAction.REFRESHKEY);
    }
}
