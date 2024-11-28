package com.accounting.core.tasks;

import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.jobs.DataFixProcessor;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;

import javax.ws.rs.WebApplicationException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static com.accounting.commons.tasks.TasksSwitcher.getSingleMandatoryParameter;

public class CreateTestAccountTask extends Task {

    private final DataFixProcessor dataFixProcessor;
    private final String ACCOUNT_CODE = "accountCode";
    private final String ACCOUNT_TYPE_CODE = "accountTypeCode";

    @Inject
    public CreateTestAccountTask(DataFixProcessor dataFixProcessor) {
        super("create-test-account");
        this.dataFixProcessor = dataFixProcessor;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws WebApplicationException, JobProcessingException {
        String accountCodeParams = getSingleMandatoryParameter(parameters, ACCOUNT_CODE);
        String accountTypeCodeParams = getSingleMandatoryParameter(parameters, ACCOUNT_TYPE_CODE);

        dataFixProcessor.createTestAccountAsCopyOf(accountCodeParams, accountTypeCodeParams);
    }
}