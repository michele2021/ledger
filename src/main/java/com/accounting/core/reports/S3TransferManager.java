package com.accounting.core.reports;

import com.accounting.commons.aws.S3FilesManager;
import com.accounting.commons.aws.S3FilesManagerImpl;
import com.accounting.commons.conf.AWSConfig;
import com.accounting.commons.exceptions.FileManagerException;
import com.accounting.commons.model.S3File;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.LocalDate;

import static java.lang.String.format;

@Slf4j
@Singleton
@AllArgsConstructor
final class S3TransferManager {

    public static final String ERROR_DIR = "ERROR";

    private final AWSConfig awsConfig;
    private final S3FilesManager s3FilesManager;

    @Inject
    public S3TransferManager(AWSConfig awsConfig) {
        this.awsConfig = awsConfig;
        this.s3FilesManager = new S3FilesManagerImpl(awsConfig, DailySalesReportUtils.LOG_PREFIX);
    }

    public static String getProcessedReportsDirectory() {
        LocalDate now = LocalDate.now();
        return format("PROCESSED/%s/%s" , now.getYear(), now.getMonth());
    }

    public S3File downloadFileByPartialKey(String fileName) throws FileManagerException {
        return s3FilesManager.downloadFileByPartialKey(awsConfig.getDataBucket(), resolvePath(fileName));
    }

    public void moveToProcessedDir(String reportName) throws FileManagerException {
        moveReport(getProcessedReportsDirectory(), reportName);
    }

    public void moveToErrorDir(String reportName) throws FileManagerException {
        moveReport(ERROR_DIR, reportName);
    }

    private void moveReport(String destinationDirectory, String reportName) {
        log.info("{}Moving file {} to {} directory started..", DailySalesReportUtils.LOG_PREFIX, reportName, destinationDirectory);
        copyToDirectory(destinationDirectory, reportName);
        deleteFile(reportName);
        log.info("{}Moving file {} to {} directory completed.", DailySalesReportUtils.LOG_PREFIX, reportName, destinationDirectory);
    }

    private void copyToDirectory(String destinationDirectory, String fileName) throws FileManagerException {
        String destinationKey = resolvePath(destinationDirectory);
        destinationKey = Path.of(destinationKey).resolve(fileName).toString();

        fileName = resolvePath(fileName);
        s3FilesManager.copyToDirectory(awsConfig.getDataBucket(), destinationKey, fileName);
    }

    private void deleteFile(String fileName) throws FileManagerException {
        s3FilesManager.deleteFile(awsConfig.getDataBucket(), resolvePath(fileName));
    }

    private String resolvePath(String fileName) {
        String reportsDirectory = awsConfig.getEpayIncomingDailySalesReportsDirectory();
        return Path.of(reportsDirectory).resolve(fileName).toString();
    }

}
