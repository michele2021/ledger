package com.accounting.core.dto;

import com.accounting.commons.entities.dto.AmountDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

@Value
public class PaymentDTO {

    @Schema(description = "Originator bank account/virtual iban", required = true)
    long fromAccountId;

    @Schema(description = "Destination bank account/virtual iban", required = true)
    long toAccountId;

    @Schema(description = "Payment amount", required = true)
    AmountDTO amount;

    @Schema(description = "Payment description", required = true)
    String description;

}
