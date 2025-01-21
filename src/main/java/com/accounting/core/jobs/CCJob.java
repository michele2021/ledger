package com.accounting.core.jobs;

import com.accounting.commons.infra.jobs.CCJobParams;

public interface CCJob {

    void process(CCJobParams params);

    String getCompletedMessage();
}