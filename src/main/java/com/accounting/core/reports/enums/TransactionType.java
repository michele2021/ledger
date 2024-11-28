package com.accounting.core.reports.enums;

import com.accounting.commons.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TransactionType {
    A(Constants.JournalType.Activated),
    D(Constants.JournalType.Cancelled);

    @Getter
    private final Constants.JournalType journalType;

}
