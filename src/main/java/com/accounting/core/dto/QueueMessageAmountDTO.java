package com.accounting.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class QueueMessageAmountDTO {

    @Schema(description = "Amount to be booked.",
            minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "123.45")
    BigDecimal quantity;

    @Schema(description = "Currency ISO code.",
            minLength = 3, requiredMode = Schema.RequiredMode.REQUIRED ,
            example = "EUR")
    String currencyCode;

}
