package com.accounting.core.dto;

import com.accounting.commons.Constants.RegisterType;
import com.accounting.commons.Constants.LineType;
import com.accounting.commons.Constants.Currencies;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record JournalLinePostingDTO(
        @NotNull(message = "Register type is required")
        RegisterType registerType,

        @NotNull(message = "Entry type (debit/credit) is required")
        LineType entryType,

        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        Currencies currency
) {}