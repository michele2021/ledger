package com.accounting.core.entities;

import com.accounting.core.reports.enums.ErrorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MismatchedToken {

    private Long txId;
    private Long tokenTxId;
    private String txReference;
    private String errorMessage;
    private ErrorStatus status;

}
