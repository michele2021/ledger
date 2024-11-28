package com.accounting.core.resources;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import io.dropwizard.db.DataSourceFactory;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.Jdbi;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DatabaseHealthCheck extends HealthCheck {

    private final Jdbi jdbi;
    private final DataSourceFactory dataSourceFactory;

    @Override
    protected HealthCheck.Result check() {
        // DataSourceFactory provides a default in case this is missing in the config file
        String query = dataSourceFactory.getValidationQuery().get();
        String ret = jdbi.withHandle(handle -> handle.createQuery(query).mapTo(String.class).one());
        return ret.equals("1") ? HealthCheck.Result.healthy() : HealthCheck.Result.unhealthy("query returned " + ret + "rows.");
    }
}
