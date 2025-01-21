package com.accounting.core.config.guice;

import com.accounting.commons.conf.AWSConfig;
import com.accounting.commons.conf.DebugToolsConfig;
import com.accounting.commons.conf.KafkaConfig;
import com.accounting.commons.db.QueueDAO;
import com.accounting.commons.infra.cache.*;
import com.accounting.commons.infra.cache.management.CacheActionDAO;
import com.accounting.commons.infra.jobs.JobsDAO;
import com.accounting.commons.kafka.KafkaConsumerFactory;
import com.accounting.commons.kafka.KafkaMessageConsumer;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.accounting.commons.kafka.KafkaProducerFactory;
import com.accounting.commons.kafka.impl.*;
import com.accounting.commons.queue.MessagesSkipperService;
import com.accounting.commons.queue.MessagesSkipperServiceImpl;
import com.accounting.commons.repository.*;
import com.accounting.commons.repository.database.*;
import com.accounting.core.config.AccountingCoreAppConfig;
import com.accounting.core.db.*;
import com.accounting.core.processing.TemplateCache;
import com.accounting.core.queue.KafkaAccountingMessagesQueue;
import com.accounting.core.services.AccountService;
import com.accounting.core.services.AccountServiceImpl;
import com.accounting.core.services.TemplateExecutionService;
import com.accounting.core.services.TemplateExecutionServiceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.postgres.PostgresPlugin;

public class AccountingCoreModule extends AbstractModule {

    private final Jdbi jdbi;
    private final KafkaAccountingMessagesQueue accountingMessagesQueue;
    private final AccountingCoreAppConfig config;
    private final TemplateCache templateCache;

    public AccountingCoreModule(Environment environment, AccountingCoreAppConfig config) {
        this.config = config;
        this.jdbi = initJdbi(environment, config.getDataSourceFactory());
        this.accountingMessagesQueue = KafkaAccountingMessagesQueue.of(config.getKafkaConfig());
        this.templateCache = new TemplateCache();
    }

    private Jdbi initJdbi(Environment environment, DataSourceFactory dataSourceFactory) {
        var factory = new JdbiFactory();
        var jdbiInstance = factory.build(environment, dataSourceFactory, "postgresql");
        jdbiInstance.installPlugin(new PostgresPlugin());
        jdbiInstance.installPlugin(new Gson2Plugin());
        return jdbiInstance;
    }

    @Provides
    public AWSConfig provideAWSConfig() {
        return config.getAwsConfig();
    }

    @Provides
    public DebugToolsConfig provideDebugToolsConfig() {
        return config.getDebugToolsConfig();
    }

    @Provides
    public TemplateCache provideTemplateCache() {
        return templateCache;
    }

    @Provides
    public KafkaConfig provideKafkaConfig() {
        return config.getKafkaConfig();
    }

    @Provides
    public KafkaAccountingMessagesQueue provideSQSAccountingMessagesQueue() {
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
        configureRepositories();
        configureCaching();
        configureKafka();
        configureServices();
        templateCache.load();
    }

    private void configureCaching() {
        if (config.getRepositoryConfig().getCache()) {
            bindRepositoriesToCache();
        } else {
            bindRepositoriesToDAO();
        }
    }

    private void configureRepositories() {
        var accountDAO = jdbi.onDemand(AccountDAO.class);
        var accountTypeDAO = jdbi.onDemand(AccountTypeDAO.class);
        var txTypeDAO = jdbi.onDemand(TxTypeDAO.class);
        var journalTypeDAO = jdbi.onDemand(JournalTypeDAO.class);
        var registerTypeDAO = jdbi.onDemand(RegisterTypeDAO.class);
        var currencyDAO = jdbi.onDemand(CurrencyDAO.class);
        var subsystemDAO = jdbi.onDemand(SubsystemDAO.class);
        var txVariantDAO = jdbi.onDemand(TxVariantDAO.class);
        var feeBaseDAO = jdbi.onDemand(FeeBaseDAO.class);

        bind(AccountCache.class).toInstance(new AccountCache(accountDAO));
        bind(AccountTypeCache.class).toInstance(new AccountTypeCache(accountTypeDAO));
        bind(TxTypeCache.class).toInstance(new TxTypeCache(txTypeDAO));
        bind(JournalTypeCache.class).toInstance(new JournalTypeCache(journalTypeDAO));
        bind(RegisterTypeCache.class).toInstance(new RegisterTypeCache(registerTypeDAO));
        bind(CurrencyCache.class).toInstance(new CurrencyCache(currencyDAO));
        bind(SubsystemCache.class).toInstance(new SubsystemCache(subsystemDAO));
        bind(TxVariantCache.class).toInstance(new TxVariantCache(txVariantDAO));
        bind(FeeBaseCache.class).toInstance(new FeeBaseCache(feeBaseDAO));
    }

    private void configureKafka() {
        bind(KafkaProducerFactory.class).to(KafkaProducerFactoryImpl.class);
        bind(new TypeLiteral<KafkaMessageProducer<String, String>>(){}).to(KafkaMessageProducerImpl.class);
        bind(KafkaConsumerFactory.class).to(KafkaConsumerFactoryImpl.class);
        bind(new TypeLiteral<KafkaMessageConsumer<String>>(){})
                .annotatedWith(Names.named("KafkaMessageConsumer"))
                .to(KafkaMessageConsumerImpl.class);
        bind(new TypeLiteral<KafkaMessageConsumer<String>>(){})
                .annotatedWith(Names.named("KafkaDLQMessageConsumer"))
                .to(KafkaDLQMessageConsumerImpl.class);
    }

    private void configureServices() {
        bind(MessagesSkipperService.class).to(MessagesSkipperServiceImpl.class);
        bind(TemplateExecutionService.class).to(TemplateExecutionServiceImpl.class);
        bind(AccountService.class).to(AccountServiceImpl.class);
    }

    private void bindRepositoriesToDAO() {
        bind(JournalTypeRepoIf.class).toInstance(new JournalTypeRepoDAO(jdbi.onDemand(JournalTypeDAO.class)));
        bind(RegisterTypeRepoIf.class).toInstance(new RegisterTypeRepoDAO(jdbi.onDemand(RegisterTypeDAO.class)));
        bind(TxTypeRepoIf.class).toInstance(new TxTypeRepoDAO(jdbi.onDemand(TxTypeDAO.class)));
        bind(CurrencyRepoIf.class).toInstance(new CurrencyRepoDAO(jdbi.onDemand(CurrencyDAO.class)));
        bind(SubsystemRepoIf.class).toInstance(new SubsystemRepoDAO(jdbi.onDemand(SubsystemDAO.class)));
        bind(AccountRepoIf.class).toInstance(new AccountRepoDAO(jdbi.onDemand(AccountDAO.class)));
        bind(AccountTypeRepoIf.class).toInstance(new AccountTypeRepoDAO(jdbi.onDemand(AccountTypeDAO.class)));
        bind(TxVariantRepoIf.class).toInstance(new TxVariantRepoDAO(jdbi.onDemand(TxVariantDAO.class)));
        bind(FeeBaseRepoIf.class).toInstance(new FeeBaseRepoDAO(jdbi.onDemand(FeeBaseDAO.class)));
    }

    private void bindRepositoriesToCache() {
        bind(JournalTypeRepoIf.class).toInstance(new JournalTypeCache(jdbi.onDemand(JournalTypeDAO.class)));
        bind(RegisterTypeRepoIf.class).toInstance(new RegisterTypeCache(jdbi.onDemand(RegisterTypeDAO.class)));
        bind(TxTypeRepoIf.class).toInstance(new TxTypeCache(jdbi.onDemand(TxTypeDAO.class)));
        bind(CurrencyRepoIf.class).toInstance(new CurrencyCache(jdbi.onDemand(CurrencyDAO.class)));
        bind(SubsystemRepoIf.class).toInstance(new SubsystemCache(jdbi.onDemand(SubsystemDAO.class)));
        bind(AccountRepoIf.class).toInstance(new AccountCache(jdbi.onDemand(AccountDAO.class)));
        bind(AccountTypeRepoIf.class).toInstance(new AccountTypeCache(jdbi.onDemand(AccountTypeDAO.class)));
        bind(TxVariantRepoIf.class).toInstance(new TxVariantCache(jdbi.onDemand(TxVariantDAO.class)));
        bind(FeeBaseRepoIf.class).toInstance(new FeeBaseCache(jdbi.onDemand(FeeBaseDAO.class)));
    }
}
