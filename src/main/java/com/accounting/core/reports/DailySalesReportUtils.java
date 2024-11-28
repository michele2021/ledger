package com.accounting.core.reports;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;

public class DailySalesReportUtils {

    public static final String LOG_PREFIX = "DAILY_SALES_REPORT: ";
    private static final String REPORT_PREFIX = "EPAYIT_D-SALES-REPORT_";

    public static String getPartialReportName() {
        LocalDate now = LocalDate.now();
        String currentDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return format("%s%s" , REPORT_PREFIX, currentDate);
    }

}
