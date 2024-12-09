package com.accounting.core.reports.enums;

import com.accounting.commons.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductType {

    PIN("Pin On Receipt", Constants.TokenType.PoR),
    POSA("Point Of Sale Activation", Constants.TokenType.POSA);

    private final String description;

    private final Constants.TokenType tokenType;
}
