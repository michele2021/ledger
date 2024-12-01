package com.accounting.core.tasks;

import com.accounting.commons.infra.cache.AccountCache;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;

import jakarta.ws.rs.WebApplicationException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static com.accounting.commons.tasks.TasksSwitcher.getMandatoryParameters;

public class ClearCacheTask extends Task {

    private final AccountCache accountCache;

    @Inject
    public ClearCacheTask(AccountCache accountCache) {
        super("clear-cache");
        this.accountCache = accountCache;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws WebApplicationException {
        for (String name : getMandatoryParameters(parameters, "name")) {
            // TODO make it more generic if/when we add more caches
            if ("ACCOUNT".equalsIgnoreCase(name)) {
                accountCache.clear();
            }
        }
    }

}
