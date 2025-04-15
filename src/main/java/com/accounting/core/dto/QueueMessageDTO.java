package com.accounting.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueueMessageDTO {

    @Schema(description = "Message type, determine how some other fields are handled.",
            allowableValues = {"PAYMENT_RECEIVED", "PAYMENT_RECEIVED_CORRECTION", "CLIENT_FEE_PAYOUT"}, required = true)
    String type;

    @Schema(description = "Code of account, account must exist in database.", required = true)
    String accountCode;

    @Schema(description = "Amount to be booked.")
    QueueMessageAmountDTO amount;

    @Schema(description = "The unique identifier of the card token. This can be any string but must be globally unique in the API Client systems. Our recommended and preferred format is UUID V4.")
    String uuid;

    @Schema(description = "Transaction reference, ex. card token id.")
    String txReference;

    @Schema(description = "Additional data, exact format depends on the message type.")
    JsonNode additionalData;

    @Schema(description = "Source of message, must be FRONT_END_APP",
            allowableValues = "FRONT_END_APP", required = true)
    String source;

    @Schema(description = "Timestamp, used only to circumvent SQS deduplication.", hidden = true)
    Long timestamp;
}