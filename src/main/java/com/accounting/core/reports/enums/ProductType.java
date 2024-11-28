package com.accounting.core.reports.enums;

import com.accounting.commons.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProductType {

    PIN("Pin On Receipt", Constants.TokenType.PoR),
    POSA("Point Of Sale Activation", Constants.TokenType.POSA);

    @Getter
    private final String description;

    @Getter
    private final Constants.TokenType tokenType;

}
