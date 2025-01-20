package com.accounting.core;

import com.accounting.commons.infra.cache.AccountCache;
import com.accounting.commons.kafka.KafkaMessageConsumer;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.accounting.commons.queue.MessageQueueClient;
import com.accounting.commons.queue.MessagesSkipperServiceImpl;
import com.accounting.commons.services.SystemNotificationsService;
import com.accounting.commons.tasks.TasksNames;
import com.accounting.core.admin.AdminResourceBundle;
import com.accounting.core.admin.QueueAdminResource;
import com.accounting.core.config.AccountingCoreAppConfig;
import com.accounting.core.config.guice.AccountingCoreModule;
import com.accounting.core.config.guice.SecurityModule;
import com.accounting.core.jobs.DataFixProcessor;
import com.accounting.core.processing.MessageProcessor;
import com.accounting.core.reports.DailySalesReportsProcessor;
import com.accounting.core.resources.*;
import com.accounting.core.services.BatchService;
import com.accounting.core.services.RepositoryHelper;
import com.accounting.core.swagger.SwaggerConfig;
import com.accounting.core.swagger.SwaggerResource;
import com.accounting.core.tasks.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountingCoreApp extends Application<AccountingCoreAppConfig> {

    private final AdminResourceBundle adminResource = new AdminResourceBundle();

    public static void main(String[] args) throws Exception {
        new AccountingCoreApp().run(args);
    }

    @Override
    public String getName() {
        return "Accounting Core Application";
    }

    @Override
    public void initialize(Bootstrap<AccountingCoreAppConfig> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false).setEnableSubstitutionInVariables(true)));

        SwaggerConfig.addSwaggerBundle(bootstrap);
        bootstrap.addBundle(adminResource);
    }

    @Override
    public void run(AccountingCoreAppConfig config, Environment environment) {
        var injector = Guice.createInjector(new AccountingCoreModule(environment, config), new SecurityModule(config));
        environment.jersey().register(injector.getInstance(SwaggerResource.class));
        environment.jersey().register(injector.getInstance(AccountingQueuesResource.class));
        environment.jersey().register(injector.getInstance(TemplateExecutionResource.class));
        environment.jersey().register(injector.getInstance(AccountResource.class));

        environment.healthChecks().register("database", injector.getInstance(DatabaseHealthCheck.class));
        environment.healthChecks().register("app", injector.getInstance(AppHealthCheck.class));

        //CORS config init
        config.getCorsConfig().init(environment);

        log.info("Initialising accounting config caches and running config validations...");
        RepositoryHelper repositoryHelper = injector.getInstance(RepositoryHelper.class);
        repositoryHelper.initAccountingConfig();
        repositoryHelper.validateEnvVars();

        // MessageQueueClient
        registerMessagesQueueClient(environment, injector, config);

        // tasks
        log.info("Adding admin tasks...");
        environment.admin().addTask(new ClearCacheTask(injector.getInstance(AccountCache.class)));
        environment.admin().addTask(new RepositoryConfigTask());
        environment.admin().addTask(new DailySalesReportTask(injector.getInstance(DailySalesReportsProcessor.class)));
        environment.admin().addTask(new CreateTestAccountTask(injector.getInstance(DataFixProcessor.class)));
        environment.admin().addTask(new DataManagerTask(injector.getInstance(DataFixProcessor.class)));
        environment.admin().addTask(new BatchManagerTask(injector.getInstance(BatchService.class)));
    }

    private void registerMessagesQueueClient(Environment environment, Injector injector, AccountingCoreAppConfig config) {
        var awsConfig = config.getAwsConfig();
        if (!awsConfig.isQueueEnabled()) {
            return;
        }
        log.info("Preparing message queue client...");
        var messageProcessor = injector.getInstance(MessageProcessor.class);
        var queuesService = injector.getInstance(MessagesSkipperServiceImpl.class);
        var notificationsService = injector.getInstance(SystemNotificationsService.class);
        var kafkaMessageProducer = injector.getInstance(new Key<KafkaMessageProducer<String, String>>() {});
        var kafkaMessageConsumer = injector.getInstance(new Key<KafkaMessageConsumer<String>>(Names.named("KafkaMessageConsumer")) {});

        var messageQueueClient = new MessageQueueClient(
                awsConfig.getQueueAccountingName(), awsConfig, config.getDebugToolsConfig(),
                environment, messageProcessor, kafkaMessageProducer, kafkaMessageConsumer, notificationsService, queuesService);

        messageQueueClient.registerQueueProcessorTask(TasksNames.QUEUE_PROCESSOR_TASK);
        messageQueueClient.registerMessagesRecorderTask(TasksNames.MESSAGES_RECORDER_TASK);

        adminResource.jersey().register(new QueueAdminResource(messageQueueClient));
    }
}
