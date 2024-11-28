package com.accounting.core.resources;

import com.codahale.metrics.health.HealthCheck;

public class AppHealthCheck extends HealthCheck {

    @Override
    protected Result check() {
        return Result.healthy();
    }
}
