package com.accounting.core.tasks;

import com.accounting.core.services.BatchService;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static com.accounting.commons.tasks.TasksSwitcher.getOptionalParameters;

@Slf4j
public class BatchManagerTask extends Task {

    private static final String BATCH_MANAGER_TASK_NAME = "batch-manager";
    private static final String ACCOUNT = "account";
    private final BatchService batchService;

    @Inject
    public BatchManagerTask(BatchService batchService) {
        super(BATCH_MANAGER_TASK_NAME);
        this.batchService = batchService;
    }

    /**
     * Parameter 'account' should contain account code, like column accountCode of the config.account table.
     * Parameter 'account' is optional and can be used multiple times to work on multiple accounts.
     */
    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        batchService.executeMaintenanceTask(getOptionalParameters(parameters, ACCOUNT));
    }
}
