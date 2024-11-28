package com.accounting.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch {

    private long batchId;
    private int registerTypeId;
    private long accountId;
    private int batchNumber;
    private LocalDate periodBeginDate;
    private LocalDate periodEndDate;
    private OffsetDateTime closedDate;
}
