package com.accounting.core.tasks;

import com.accounting.commons.SystemConstants;
import com.accounting.commons.infra.jobs.CCJobParams;
import com.accounting.core.jobs.TokenExpiryJob;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;

import jakarta.ws.rs.WebApplicationException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.accounting.commons.tasks.TasksSwitcher.addSingleOptionalParameter;

@Slf4j
public class TokenExpiryTask extends Task {

    private final TokenExpiryJob tokenExpiryJob;

    @Inject
    public TokenExpiryTask(TokenExpiryJob tokenExpiryJob) {
        super(SystemConstants.Jobs.TOKEN_EXPIRY.task);
        this.tokenExpiryJob = tokenExpiryJob;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws WebApplicationException {
        CCJobParams params = new CCJobParams();
        addSingleOptionalParameter(params, parameters, SystemConstants.JParam.txId, null);
        addSingleOptionalParameter(params, parameters, SystemConstants.JParam.test, "false");
        addSingleOptionalParameter(params, parameters, SystemConstants.JParam.recovery, "false");

        log.info("Going to submit tokenExpiryJob execution {}", System.currentTimeMillis());
        CompletableFuture.runAsync(() -> tokenExpiryJob.run(params));
        log.info("Job submitted {}", System.currentTimeMillis());
    }
}