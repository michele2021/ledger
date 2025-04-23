package com.accounting.core.config.guice;

import com.accounting.commons.resources.security.ApiKeyAuth;
import com.accounting.commons.resources.security.ApiKeyAuthInterceptor;
import com.accounting.commons.resources.security.ApiPreProcessInterceptor;
import com.accounting.commons.resources.security.PreProcess;
import com.accounting.core.config.AccountingCoreAppConfig;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

public class SecurityModule extends AbstractModule {
    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;
    private final ApiPreProcessInterceptor apiPreProcessInterceptor;

    public SecurityModule(AccountingCoreAppConfig config) {
        this.apiKeyAuthInterceptor = new ApiKeyAuthInterceptor();
        this.apiPreProcessInterceptor = new ApiPreProcessInterceptor(config.getAwsConfig());
    }

    @Override
    protected void configure() {
        bind(ApiPreProcessInterceptor.class).toInstance(apiPreProcessInterceptor);
        requestInjection(apiPreProcessInterceptor);
        bindInterceptor(Matchers.any(),
                Matchers.annotatedWith(ApiKeyAuth.class),
                apiKeyAuthInterceptor);
        bindInterceptor(Matchers.any(),
                Matchers.annotatedWith(PreProcess.class),
                apiPreProcessInterceptor);
    }
}
