package com.accounting.core.reports;

import com.accounting.commons.aws.S3FilesManagerImpl;
import com.accounting.commons.conf.AWSConfig;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3TransferManagerTest {

    private static final String REPORTS_DIRECTORY = "in/epay_it/dsr/";
    private static final String DATA_BUCKET = "clevercards-test-ledger-data";

    @Mock
    private AWSConfig awsConfig;

    @Mock
    private S3FilesManagerImpl s3FilesManager;

    @InjectMocks
    private S3TransferManager s3TransferManager;


    @BeforeEach
    void reset() {
        Mockito.reset(awsConfig, s3FilesManager);
    }

    @ParameterizedTest
    @ValueSource(strings = {"in///epay_it/dsr", "in/epay_it/dsr"})
    void downloadFile(String directoryPath) {
        // given
        String defaultReportName = DailySalesReportUtils.getPartialReportName();
        String reportName = REPORTS_DIRECTORY + defaultReportName;
        if (SystemUtils.IS_OS_WINDOWS) { // S3 use Unix paths, but path.resolve() used inside downloadFileByPartialKey() on Windows will return "\" as separator
            reportName = reportName.replaceAll("/", "\\\\");
        }

        when(awsConfig.getDataBucket()).thenReturn(DATA_BUCKET);
        when(awsConfig.getEpayIncomingDailySalesReportsDirectory()).thenReturn(directoryPath);

        // when
        s3TransferManager.downloadFileByPartialKey(defaultReportName);

        // then
        verify(awsConfig).getDataBucket();
        verify(awsConfig).getEpayIncomingDailySalesReportsDirectory();

        verify(s3FilesManager).downloadFileByPartialKey(DATA_BUCKET, reportName);
    }

    @Test
    void testMoveToProcessedDir() {
        // given
        String reportName = DailySalesReportUtils.getPartialReportName();

        final Path rdpath = Path.of(REPORTS_DIRECTORY);
        String destinationKey = rdpath
                .resolve(S3TransferManager.getProcessedReportsDirectory())
                .resolve(reportName).toString();


        when(awsConfig.getDataBucket()).thenReturn(DATA_BUCKET);
        when(awsConfig.getEpayIncomingDailySalesReportsDirectory()).thenReturn(REPORTS_DIRECTORY);

        // when
        s3TransferManager.moveToProcessedDir(reportName);

        // then
        reportName = rdpath.resolve(reportName).toString();

        verify(awsConfig, times(2)).getDataBucket();
        verify(awsConfig, times(3)).getEpayIncomingDailySalesReportsDirectory();

        verify(s3FilesManager).copyToDirectory(DATA_BUCKET, destinationKey, reportName);
        verify(s3FilesManager).deleteFile(DATA_BUCKET, reportName);
    }

    @Test
    void testMoveToErrorDir() {
        // given
        String reportName = DailySalesReportUtils.getPartialReportName();

        final Path rdpath = Path.of(REPORTS_DIRECTORY);
        String destinationKey = rdpath
                .resolve(S3TransferManager.ERROR_DIR)
                .resolve(reportName).toString();


        when(awsConfig.getDataBucket()).thenReturn(DATA_BUCKET);
        when(awsConfig.getEpayIncomingDailySalesReportsDirectory()).thenReturn(REPORTS_DIRECTORY);

        // when
        s3TransferManager.moveToErrorDir(reportName);

        // then
        reportName = rdpath.resolve(reportName).toString();

        verify(awsConfig, times(2)).getDataBucket();
        verify(awsConfig, times(3)).getEpayIncomingDailySalesReportsDirectory();

        verify(s3FilesManager).copyToDirectory(DATA_BUCKET, destinationKey, reportName);
        verify(s3FilesManager).deleteFile(DATA_BUCKET, reportName);
    }
}
