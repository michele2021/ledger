package com.accounting.core.dto;

import com.accounting.commons.Constants.RegisterType;
import com.accounting.commons.Constants.LineType;
import com.accounting.commons.Constants.Currencies;

import java.math.BigDecimal;

public record JournalLinePostingDTO(
    RegisterType registerType,
    LineType lineType,
    BigDecimal amount,
    Currencies currency
) {
}