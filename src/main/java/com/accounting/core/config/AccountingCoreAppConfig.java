package com.accounting.core.config;


import com.accounting.commons.conf.AWSConfig;
import com.accounting.commons.conf.CORSConfig;
import com.accounting.commons.conf.DebugToolsConfig;
import com.accounting.commons.conf.RepositoryConfig;
import com.accounting.core.swagger.SwaggerConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class AccountingCoreAppConfig extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory dataSourceFactory = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("aws")
    private AWSConfig AWSConfig = new AWSConfig();

    @Valid
    @NotNull
    @JsonProperty("debugTools")
    private DebugToolsConfig debugToolsConfig = new DebugToolsConfig();


    @Valid
    @NotNull
    @JsonProperty("repository")
    private RepositoryConfig repositoryConfig = new RepositoryConfig();

    @Valid
    @NotNull
    @JsonProperty("cors")
    private CORSConfig corsConfig = new CORSConfig();

    public SwaggerConfig swaggerConfig = new SwaggerConfig();

}
