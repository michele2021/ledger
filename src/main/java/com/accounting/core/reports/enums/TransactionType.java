package com.accounting.core.reports.enums;

import com.accounting.commons.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionType {
    A(Constants.JournalType.Manual),
    D(Constants.JournalType.Manual);

    private final Constants.JournalType journalType;
}
