package com.accounting.core.tasks;

import com.accounting.commons.tasks.TasksSwitcher;
import com.accounting.core.reports.DailySalesReportUtils;
import com.accounting.core.reports.DailySalesReportsProcessor;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static com.accounting.commons.tasks.TasksSwitcher.getSingleOptionalParameter;

public class DailySalesReportTask extends Task {

    public static final String REPORT_NAME_PARAM = "reportName";
    public static final String REPROCESS_PARAM = "reprocessing";

    private static final String DAILY_SALES_REPORT_TASK_NAME = "daily-sales-report";
    private final DailySalesReportsProcessor dailySalesReportsProcessor;

    @Inject
    public DailySalesReportTask(DailySalesReportsProcessor dailySalesReportsProcessor) {
        super(DAILY_SALES_REPORT_TASK_NAME);
        this.dailySalesReportsProcessor = dailySalesReportsProcessor;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {

        boolean reprocessing = getSingleOptionalParameter(parameters, REPROCESS_PARAM)
                .map(value -> TasksSwitcher.toBoolean(value, REPROCESS_PARAM))
                .orElse(false);

        String reportName = getSingleOptionalParameter(parameters, REPORT_NAME_PARAM)
                .orElse(DailySalesReportUtils.getPartialReportName());

        dailySalesReportsProcessor.processReport(reportName, reprocessing);
    }

}
