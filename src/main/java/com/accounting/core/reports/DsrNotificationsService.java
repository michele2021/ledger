package com.accounting.core.reports;

import com.accounting.commons.conf.DebugToolsConfig;
import com.accounting.commons.conf.SystemConfig;
import com.accounting.commons.services.NotificationService;
import com.accounting.commons.services.SystemNotificationsService;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.accounting.commons.Constants.TIMESTAMP_FORMATTER;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;

@Slf4j
@Singleton
class DsrNotificationsService {

    private final String environment;
    private final boolean dsrSnsNotificationEnabled;
    private final NotificationService notificationService;

    @Inject
    public DsrNotificationsService(DebugToolsConfig debugToolsConfig,
                                   SystemNotificationsService notificationService) {
        this.notificationService = notificationService;
        this.environment = SystemConfig.getEnvironment().toUpperCase();
        this.dsrSnsNotificationEnabled = debugToolsConfig.getDsrSnsNotificationEnabled();
    }

    void sendReportErrorWhenCreatingDSRTxNotification(String reportName) {
        sendToSns(() -> {
            log.info("DSR Error when creating the Daily Sales Report Tx. Publishing message to sns topic.");
            Map<String, Object> message = ImmutableMap.of(
                    "status", "Error creating DSR Tx.",
                    "timestamp", TIMESTAMP_FORMATTER.format(LocalDateTime.now(UTC)),
                    "environment", environment,
                    "reportName", reportName);
            String subject = format("Ledger %s - DSR error when creating the Daily Sales Report Tx.", environment);
            notificationService.sendNotification(message, subject);
        });
    }

    void sendReportFileNotFoundNotification(String partialReportName) {
        sendToSns(() -> {
            log.info("DSR file not found. Publishing message to sns topic.");
            Map<String, Object> message = ImmutableMap.of(
                    "status", "DSR file not found.",
                    "timestamp", TIMESTAMP_FORMATTER.format(LocalDateTime.now(UTC)),
                    "environment", environment,
                    "reportName", partialReportName);
            String subject = format("Ledger %s - DSR file not found.", environment);
            notificationService.sendNotification(message, subject);
        });
    }

    void sendReportProcessedWithErrorsNotification(List<String> errors, String fullReportName) {
        sendToSns(() -> {
            log.info("Daily Sales Report processing ended with errors. Publishing message to sns topic.");
            Map<String, Object> message = ImmutableMap.of(
                    "status", "Processing DSR file ended with errors.",
                    "timestamp", TIMESTAMP_FORMATTER.format(LocalDateTime.now(UTC)),
                    "environment", environment,
                    "reportName", fullReportName,
                    "errors", errors);
            String subject = format("Ledger %s - DSR processed with errors.", environment);
            notificationService.sendNotification(message, subject);
        });
    }

    private void sendToSns(Runnable runnable) {
        if (dsrSnsNotificationEnabled) {
            runnable.run();
            return;
        }
        log.info("DSR notifications disabled. Publishing message to SNS skipped.");
    }

}
