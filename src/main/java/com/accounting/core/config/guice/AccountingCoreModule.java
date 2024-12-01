package com.accounting.core.config.guice;

import com.accounting.commons.conf.AWSConfig;
import com.accounting.commons.conf.DebugToolsConfig;
import com.accounting.commons.db.QueueDAO;
import com.accounting.commons.infra.cache.*;
import com.accounting.commons.infra.cache.management.CacheActionDAO;
import com.accounting.commons.infra.jobs.JobsDAO;
import com.accounting.commons.queue.MessagesSkipperService;
import com.accounting.commons.queue.MessagesSkipperServiceImpl;
import com.accounting.commons.repository.*;
import com.accounting.commons.repository.database.*;
import com.accounting.core.config.AccountingCoreAppConfig;
import com.accounting.core.db.*;
import com.accounting.core.processing.TemplateCache;
import com.accounting.core.queue.SQSAccountingMessagesQueue;
import com.accounting.core.services.AccountService;
import com.accounting.core.services.AccountServiceImpl;
import com.accounting.core.services.TemplateExecutionService;
import com.accounting.core.services.TemplateExecutionServiceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.postgres.PostgresPlugin;

public class AccountingCoreModule extends AbstractModule {

    private final Jdbi jdbi;
   // private final SQSAccountingBalanceQueue accountingBalanceQueue;
    private final SQSAccountingMessagesQueue accountingMessagesQueue;
    private final AccountingCoreAppConfig config;
    private final TemplateCache templateCache;

    public AccountingCoreModule(Environment environment, AccountingCoreAppConfig config) {
        this.config = config;
        final JdbiFactory factory = new JdbiFactory();
        this.jdbi = factory.build(environment, config.getDataSourceFactory(), "postgresql");
        jdbi.installPlugin(new PostgresPlugin());
        jdbi.installPlugin(new Gson2Plugin());
        //this.accountingBalanceQueue = SQSAccountingBalanceQueue.of(config.getAWSConfig());
        this.accountingMessagesQueue = SQSAccountingMessagesQueue.of(config.getAWSConfig());
        this.templateCache = new TemplateCache();
    }

    @Provides
    public AWSConfig provideAWSConfig() {
        return config.getAWSConfig();
    }

    @Provides
    public DebugToolsConfig provideDebugToolsConfig() {
        return config.getDebugToolsConfig();
    }

    @Provides
    public TemplateCache provideTemplateCache() {
        return templateCache;
    }

//    @Provides
//    public SQSAccountingBalanceQueue provideSQSAccountingBalanceQueue() {
//        return accountingBalanceQueue;
//    }

    @Provides
    public SQSAccountingMessagesQueue provideSQSAccountingMessagesQueue() {
        return accountingMessagesQueue;
    }

    @Provides
    public AccountDAO provideAccountDAO() {
        return jdbi.onDemand(AccountDAO.class);
    }

    @Provides
    public AccountWriteDAO provideAccountWriteDAO() {
        return jdbi.onDemand(AccountWriteDAO.class);
    }

    @Provides
    public CurrencyDAO provideCurrencyDAO() {
        return jdbi.onDemand(CurrencyDAO.class);
    }

    @Provides
    public JournalDAO provideJournalDAO() {
        return jdbi.onDemand(JournalDAO.class);
    }

    @Provides
    public BatchDAO provideBatchDAO() {
        return jdbi.onDemand(BatchDAO.class);
    }

    @Provides
    public Jdbi provideJdbi() {
        return jdbi;
    }

    @Provides
    public QueueDAO provideQueueDAO() {
        return jdbi.onDemand(QueueDAO.class);
    }

    @Provides
    public TxTypeDAO provideTxTypeDAO() {
        return jdbi.onDemand(TxTypeDAO.class);
    }

    @Provides
    public SubsystemDAO provideSubsystemDAO() {
        return jdbi.onDemand(SubsystemDAO.class);
    }

    @Provides
    public DataSourceFactory provideDataSourceFactory() {
        return config.getDataSourceFactory();
    }

    @Provides
    public MismatchedTokensDAO provideMismatchedTokensDAO() {
        return jdbi.onDemand(MismatchedTokensDAO.class);
    }

    @Provides
    public DataFixDAO DataFixDAO() {
        return jdbi.onDemand(DataFixDAO.class);
    }

    @Provides
    public CacheActionDAO CacheActionDAO() {
        return jdbi.onDemand(CacheActionDAO.class);
    }

    @Provides
    public TokenExpiryDAO provideTokenExpiryDAO() {
        return jdbi.onDemand(TokenExpiryDAO.class);
    }

    @Provides
    public JobsDAO provideJobDAO() {
        return jdbi.onDemand(JobsDAO.class);
    }

    @Override
    protected void configure() {
        RegisterTypeRepoIf registerTypeRepo;
        JournalTypeRepoIf journalTypeRepo;
        TxTypeRepoIf txTypeRepo;
        CurrencyRepoIf currencyRepo;
        SubsystemRepoIf subsystemRepo;
        AccountRepoIf accountRepo;
        AccountTypeRepoIf accountTypeRepo;
        TxVariantRepoIf txVariantRepo;
        FeeBaseRepoIf feeBaseRepo;

        //
        JournalTypeDAO journalTypeDAO = jdbi.onDemand(JournalTypeDAO.class);
        RegisterTypeDAO registerTypeDAO = jdbi.onDemand(RegisterTypeDAO.class);
        TxTypeDAO txTypeDAO = jdbi.onDemand(TxTypeDAO.class);
        CurrencyDAO currencyDAO = jdbi.onDemand(CurrencyDAO.class);
        SubsystemDAO subsystemDAO = jdbi.onDemand(SubsystemDAO.class);
        AccountDAO accountDAO = jdbi.onDemand(AccountDAO.class);
        AccountTypeDAO accountTypeDAO = jdbi.onDemand(AccountTypeDAO.class);
        TxVariantDAO txVariantDAO = jdbi.onDemand(TxVariantDAO.class);
        FeeBaseDAO feeBaseDAO = jdbi.onDemand(FeeBaseDAO.class);
        AccountCache accountCache = new AccountCache(accountDAO);
        //
        AccountTypeCache accountTypeCache = new AccountTypeCache(accountTypeDAO);
        TxVariantCache txVariantCache = new TxVariantCache(txVariantDAO);
        SubsystemCache subsystemCache = new SubsystemCache(subsystemDAO);
        CurrencyCache currencyCache = new CurrencyCache(currencyDAO);
        TxTypeCache txTypeCache = new TxTypeCache(txTypeDAO);
        RegisterTypeCache registerTypeCache = new RegisterTypeCache(registerTypeDAO);
        JournalTypeCache journalTypeCache = new JournalTypeCache(journalTypeDAO);
        FeeBaseCache feeBaseCache = new FeeBaseCache(feeBaseDAO);
        //
        bind(AccountCache.class).toInstance(accountCache);
        bind(AccountTypeCache.class).toInstance(accountTypeCache);
        bind(SubsystemCache.class).toInstance(subsystemCache);
        bind(CurrencyCache.class).toInstance(currencyCache);
        bind(TxTypeCache.class).toInstance(txTypeCache);
        bind(RegisterTypeCache.class).toInstance(registerTypeCache);
        bind(JournalTypeCache.class).toInstance(journalTypeCache);
        bind(FeeBaseCache.class).toInstance(feeBaseCache);
        //
        if (config.getRepositoryConfig().getCache()) {
            journalTypeRepo = journalTypeCache;
            registerTypeRepo = registerTypeCache;
            txTypeRepo = txTypeCache;
            currencyRepo = currencyCache;
            subsystemRepo = subsystemCache;
            accountRepo = accountCache;
            accountTypeRepo = accountTypeCache;
            txVariantRepo = txVariantCache;
            feeBaseRepo = feeBaseCache;
        } else {
            journalTypeRepo = new JournalTypeRepoDAO(journalTypeDAO);
            registerTypeRepo = new RegisterTypeRepoDAO(registerTypeDAO);
            txTypeRepo = new TxTypeRepoDAO(txTypeDAO);
            currencyRepo = new CurrencyRepoDAO(currencyDAO);
            subsystemRepo = new SubsystemRepoDAO(subsystemDAO);
            accountRepo = new AccountRepoDAO(accountDAO);
            accountTypeRepo = new AccountTypeRepoDAO(accountTypeDAO);
            txVariantRepo = new TxVariantRepoDAO(txVariantDAO);
            feeBaseRepo = new FeeBaseRepoDAO(feeBaseDAO);
        }
        bind(JournalTypeRepoIf.class).toInstance(journalTypeRepo);
        bind(RegisterTypeRepoIf.class).toInstance(registerTypeRepo);
        bind(TxTypeRepoIf.class).toInstance(txTypeRepo);
        bind(CurrencyRepoIf.class).toInstance(currencyRepo);
        bind(SubsystemRepoIf.class).toInstance(subsystemRepo);
        bind(AccountRepoIf.class).toInstance(accountRepo);
        bind(AccountTypeRepoIf.class).toInstance(accountTypeRepo);
        bind(TxVariantRepoIf.class).toInstance(txVariantRepo);
        bind(FeeBaseRepoIf.class).toInstance(feeBaseRepo);
        bind(MessagesSkipperService.class).to(MessagesSkipperServiceImpl.class);
        bind(TemplateExecutionService.class).to(TemplateExecutionServiceImpl.class);
        bind(AccountService.class).to(AccountServiceImpl.class);

        templateCache.load();
    }
}
