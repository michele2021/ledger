package com.accounting.core.jobs;

import com.accounting.commons.SystemConstants;
import com.accounting.commons.infra.jobs.CCJobParams;
import com.accounting.commons.infra.jobs.JobsDAO;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Slf4j
public abstract class BaseJob implements CCJob {

    protected SystemConstants.Jobs name;
    protected JobsDAO jobsDAO;
    protected String message = "";

    public void run(CCJobParams params) {
        long jobId = init(params);
        long ini, end, millis;
        ini = System.currentTimeMillis();
        try {
            process(params);
        } catch (Exception e) {
            end = System.currentTimeMillis();
            millis = end - ini;
            completedError(jobId, millis, e);
            return;
        }
        end = System.currentTimeMillis();
        millis = end - ini;
        completedSuccess(jobId, millis);
    }

    private long init(CCJobParams params) {
        log.info("Job {} execution started.", name);
        message = "";
        try {
             return jobsDAO.insertJob(name, null, params.toJsonString(), OffsetDateTime.now(), SystemConstants.JobStatus.RUNNING, null);
        } catch (Exception e) {
            log.error("Exception while creating a job entry in the database.", e);
            throw e;
        }
    }

    @Override
    public String getCompletedMessage() {
        return message;
    }

    private void completedSuccess(long jobId, long millis) {
        log.info("Job {} execution [{}] completed successfully in {} minutes.", name, jobId, TimeUnit.MILLISECONDS.toMinutes(millis));
        jobsDAO.updateJob(jobId, SystemConstants.JobStatus.DONE, getCompletedMessage());
    }

    private void completedError(long jobId, long millis, Exception e) {
        log.warn("Job {} execution [{}] terminated with exception after {} minutes.", name, jobId, TimeUnit.MILLISECONDS.toMinutes(millis));
        log.warn("Exception: ", e);
        String message = e.getMessage().replace("\n", " ");
        jobsDAO.updateJob(jobId, SystemConstants.JobStatus.ERROR, message);
    }

    void info(String msg, Object... args) {
        String m = format(msg, args);
        message = message.concat(m);
        log.info(m);
    }
}