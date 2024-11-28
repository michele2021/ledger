package com.accounting.core.jobs.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExecution {
    private String name;
    private String params;
    private String status;
    private String message;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}