package com.accounting.core.reports;

import com.accounting.commons.conf.AWSConfig;
import com.accounting.commons.model.S3File;
import com.accounting.core.exceptions.FileParsingException;
import com.accounting.core.reports.model.DailySalesReport;
import com.accounting.core.reports.model.DailySalesReportRow;
import com.accounting.core.testUtils.TestDailySalesReportsUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySalesReportsImporterTest {

    private static final String TEST_BUCKET_NAME = "test-bucket";
    private static final String TEST_REPORT_PARTIAL_NAME = "test-report-partial-name";
    private static final String TEST_REPORT_FULL_NAME = "test-report-full-name";

    @Mock
    private AWSConfig awsConfig;

    @Mock
    private DailySalesReportsCSVMapper csvMapper;

    @Mock
    private S3TransferManager s3TransferManager;

    @InjectMocks
    private DailySalesReportsImporter dailySalesReportsImporter;

    @BeforeEach
    void setUp() {
        when(awsConfig.getDataBucket()).thenReturn(TEST_BUCKET_NAME);
    }

    @AfterEach
    void reset() {
        Mockito.reset(awsConfig, s3TransferManager, csvMapper);
    }


    @Test
    void moveToProcessedDir() {
        // when
        dailySalesReportsImporter.moveToProcessedDir(TEST_REPORT_FULL_NAME);

        // then
        verify(s3TransferManager).moveToProcessedDir(TEST_REPORT_FULL_NAME);
    }

    @Test
    void moveToErrorDir() {
        // when
        dailySalesReportsImporter.moveToErrorDir(TEST_REPORT_FULL_NAME);

        // then
        verify(s3TransferManager).moveToErrorDir(TEST_REPORT_FULL_NAME);
    }

    @Test
    void testImportReportSuccess() throws IOException, FileParsingException {
        // given
        long rowNumber = 1L;
        DailySalesReportRow expectedRow = TestDailySalesReportsUtils.getReportRow();
        expectedRow.setRowNumber(rowNumber);

        String testReport = TestDailySalesReportsUtils.getReportString(expectedRow);

        List<String> errors = new ArrayList<>();
        byte[] content = testReport.getBytes(StandardCharsets.UTF_8);


        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            S3File s3File = new S3File(TEST_REPORT_FULL_NAME, inputStream);
            when(s3TransferManager.downloadFileByPartialKey(TEST_REPORT_PARTIAL_NAME)).thenReturn(s3File);
        }

        when(csvMapper.map(anyString(), eq(rowNumber))).thenReturn(expectedRow);

        // when
        DailySalesReport report = dailySalesReportsImporter.importReport(TEST_REPORT_PARTIAL_NAME, errors);
        List<DailySalesReportRow> reportRows = report.getReportRows();

        // then
        assertThat(errors).isEmpty();
        assertThat(reportRows).hasSize(1);
        assertThat(expectedRow.getRowNumber()).isEqualTo(1L);
        assertThat(reportRows.getFirst()).isEqualTo(expectedRow);
        assertThat(report.getFileName()).isEqualTo(TEST_REPORT_FULL_NAME);

        verify(csvMapper).map(anyString(), eq(rowNumber));
        verify(s3TransferManager).downloadFileByPartialKey(TEST_REPORT_PARTIAL_NAME);
    }

    @Test
    void testImportReportEmptyCSV() throws IOException {
        // given
        List<String> errors = new ArrayList<>();
        byte[] csvHeaderBytes = TestDailySalesReportsUtils.CSV_HEADER.getBytes(StandardCharsets.UTF_8);

        try (InputStream inputStream = new ByteArrayInputStream(csvHeaderBytes)) {
            S3File s3File = new S3File(TEST_REPORT_FULL_NAME, inputStream);
            when(s3TransferManager.downloadFileByPartialKey(TEST_REPORT_PARTIAL_NAME)).thenReturn(s3File);
        }

        // when
        DailySalesReport report = dailySalesReportsImporter.importReport(TEST_REPORT_PARTIAL_NAME, errors);

        //then
        assertNotNull(report.getReportRows());
        assertThat(report.getReportRows()).isEmpty();
        assertThat(report.getFileName()).isEqualTo(TEST_REPORT_FULL_NAME);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).contains(format("Error - csv file '%s' contains only header row.", TEST_REPORT_FULL_NAME));

        verify(s3TransferManager).downloadFileByPartialKey(TEST_REPORT_PARTIAL_NAME);
        verifyNoInteractions(csvMapper);
    }

    @Test
    void testImportReportCSVWithSomeInvalidRows() throws IOException, FileParsingException {
        // given
        long invalidLineRowNumber1 = 1L;
        long validLineRowNumber = 2L;
        long invalidLineRowNumber2 = 3L;

        DailySalesReportRow invalidRow1 = TestDailySalesReportsUtils.getReportRow();
        DailySalesReportRow invalidRow2 = TestDailySalesReportsUtils.getReportRow();
        DailySalesReportRow expectedRow = TestDailySalesReportsUtils.getReportRow();
        expectedRow.setRowNumber(validLineRowNumber);

        String reportWithInvalidRows = TestDailySalesReportsUtils.getReportString(List.of(invalidRow1, expectedRow, invalidRow2));

        final List<String> errors = new ArrayList<>();
        byte[] content = reportWithInvalidRows.getBytes(StandardCharsets.UTF_8);
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            S3File s3File = new S3File(TEST_REPORT_FULL_NAME, inputStream);
            when(s3TransferManager.downloadFileByPartialKey(TEST_REPORT_PARTIAL_NAME)).thenReturn(s3File);
        }

        when(csvMapper.map(anyString(), anyLong()))
                .thenThrow(new FileParsingException(format("Row number [%d]. Invalid transaction type", invalidLineRowNumber1)))
                .thenReturn(expectedRow)
                .thenThrow(new FileParsingException(format("Row number [%d]. Invalid product type", invalidLineRowNumber2)));

        // when
        DailySalesReport actualReport = dailySalesReportsImporter.importReport(TEST_REPORT_PARTIAL_NAME, errors);

        //then
        assertThat(errors).hasSize(2);
        assertThat(errors.get(0)).contains(format("FileParsingException: Row number [%d]. Invalid transaction type", invalidLineRowNumber1));
        assertThat(errors.get(1)).contains(format("FileParsingException: Row number [%d]. Invalid product type", invalidLineRowNumber2));

        assertNotNull(actualReport.getReportRows());
        assertThat(actualReport.getReportRows()).hasSize(1);
        assertThat(actualReport.getReportRows().getFirst()).isEqualTo(expectedRow);
        assertEquals(TEST_REPORT_FULL_NAME, actualReport.getFileName());

        verify(s3TransferManager).downloadFileByPartialKey(TEST_REPORT_PARTIAL_NAME);
        verify(csvMapper, times(3)).map(anyString(), anyLong());
    }

    @Test
    void testImportReportCSVWithAllInvalidRows() throws IOException, FileParsingException {
        // given
        long invalidLineRowNumber1 = 1L;
        long invalidLineRowNumber2 = 2L;

        DailySalesReportRow invalidRow1 = TestDailySalesReportsUtils.getReportRow();
        DailySalesReportRow invalidRow2 = TestDailySalesReportsUtils.getReportRow();

        String reportWithInvalidRows = TestDailySalesReportsUtils.getReportString(List.of(invalidRow1, invalidRow2));

        final List<String> errors = new ArrayList<>();
        byte[] content = reportWithInvalidRows.getBytes(StandardCharsets.UTF_8);
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            S3File s3File = new S3File(TEST_REPORT_FULL_NAME, inputStream);
            when(s3TransferManager.downloadFileByPartialKey(TEST_REPORT_PARTIAL_NAME)).thenReturn(s3File);
        }

        when(csvMapper.map(anyString(), anyLong()))
                .thenThrow(new FileParsingException(format("Row number [%d]. Invalid transaction type", invalidLineRowNumber1)))
                .thenThrow(new FileParsingException(format("Row number [%d]. Invalid product type", invalidLineRowNumber2)));

        // when
        DailySalesReport actualReport = dailySalesReportsImporter.importReport(TEST_REPORT_PARTIAL_NAME, errors);

        //then
        assertThat(errors).hasSize(2);
        assertThat(errors.get(0)).contains(format("FileParsingException: Row number [%d]. Invalid transaction type", invalidLineRowNumber1));
        assertThat(errors.get(1)).contains(format("FileParsingException: Row number [%d]. Invalid product type", invalidLineRowNumber2));

        assertNotNull(actualReport.getReportRows());
        assertThat(actualReport.getReportRows()).isEmpty();
        assertEquals(TEST_REPORT_FULL_NAME, actualReport.getFileName());

        verify(s3TransferManager).downloadFileByPartialKey(TEST_REPORT_PARTIAL_NAME);
        verify(csvMapper, times(2)).map(anyString(), anyLong());
    }

}
