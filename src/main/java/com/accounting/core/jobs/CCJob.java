package com.accounting.core.jobs;

import com.accounting.commons.infra.jobs.CCJobParams;
import com.accounting.core.exceptions.JobProcessingException;

public interface CCJob {

    void process(CCJobParams params) throws JobProcessingException;

    String getCompletedMessage();
}