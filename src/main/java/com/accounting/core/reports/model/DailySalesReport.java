package com.accounting.core.reports.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesReport {

    private String fileName;
    private List<DailySalesReportRow> reportRows;
}
