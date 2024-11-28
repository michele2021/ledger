package com.accounting.core.entities.views;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenExpiryView {

    private long txId;
    private String txReference;
    private int accountId;
    private String accountCode;
    private LocalDate expiryDate;
}