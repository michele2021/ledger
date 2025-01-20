package com.accounting.core.config;


import com.accounting.commons.conf.*;
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
    private AWSConfig awsConfig = new AWSConfig();

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

    private SwaggerConfig swaggerConfig = new SwaggerConfig();

    @Valid
    @NotNull
    @JsonProperty("kafka")
    private KafkaConfig kafkaConfig = new KafkaConfig();
}
