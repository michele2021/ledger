package com.accounting.core.reports;


import com.accounting.commons.exceptions.FileManagerException;
import com.accounting.commons.model.S3File;
import com.accounting.core.exceptions.FileParsingException;
import com.accounting.core.reports.model.DailySalesReport;
import com.accounting.core.reports.model.DailySalesReportRow;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
final class DailySalesReportsImporter {

    private final S3TransferManager s3TransferManager;
    private final DailySalesReportsCSVMapper csvMapper;

    public DailySalesReport importReport(String partialReportName, List<String> errorsList) throws FileManagerException {
        return getReport(partialReportName, errorsList);
    }

    public void moveToProcessedDir(String fullReportName) throws FileManagerException {
        s3TransferManager.moveToProcessedDir(fullReportName);
    }

    public void moveToErrorDir(String fullReportName) throws FileManagerException {
        s3TransferManager.moveToErrorDir(fullReportName);
    }

    private DailySalesReport getReport(String partialReportName, List<String> errorsList) {

        S3File s3File = s3TransferManager.downloadFileByPartialKey(partialReportName);
        String fullReportName = s3File.getFileName();

        try (InputStream reportAsInputStream = s3File.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(reportAsInputStream))) {

            final AtomicLong rowCounter = new AtomicLong(1);
            List<DailySalesReportRow> dailySalesReportRows = reader.lines()
                    .skip(DailySalesReportsCSVMapper.HEADER_ROWS_NUMBER)
                    .map(line -> mapReportRow(line, rowCounter.getAndIncrement(), errorsList))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (dailySalesReportRows.isEmpty() && rowCounter.get() == 1) {
                errorsList.add(format("Error - csv file '%s' contains only header row.", fullReportName));
            }
            return new DailySalesReport(fullReportName, dailySalesReportRows);

        } catch (IOException e) {
            throw new FileManagerException(messageWithLogPrefix(format("IOException: Could not read file '%s'", fullReportName)));
        }
    }

    private DailySalesReportRow mapReportRow(String csvLine, Long rowNumber, List<String> errorsList) {
        try {
            return csvMapper.map(csvLine, rowNumber);
        } catch (FileParsingException e) {
            errorsList.add(format("FileParsingException: %s", e.getMessage()));
        }
        return null;
    }

    private String messageWithLogPrefix(String errorMsg) {
        return format("%s%s", DailySalesReportUtils.LOG_PREFIX, errorMsg);
    }
}
