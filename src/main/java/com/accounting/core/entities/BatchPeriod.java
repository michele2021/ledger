package com.accounting.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPeriod {

    private int registerTypeId;
    private long accountId;
    private String period; // Allowed values are 'M' -> monthly, 'W' -> weekly, 'D' -> daily.

}
