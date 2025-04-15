package com.accounting.core.resources;

import com.accounting.commons.SystemConstants;
import com.accounting.commons.resources.security.ApiKeyAuth;
import com.accounting.commons.resources.security.PreProcess;
import com.accounting.core.dto.JournalPostingDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.services.TemplateExecutionService;
import com.accounting.core.swagger.ApiBadRequestResponse;
import com.accounting.core.swagger.ApiDefaultResponses;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.accounting.core.swagger.ApiDocConstants.HTTP_STATUS_OK;
import static com.accounting.core.swagger.ApiDocConstants.OK_STATUS_DESCRIPTION;

@Slf4j
@Singleton
@Path("/accounting")
@ApiDefaultResponses
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Tag(name = "TemplateExecutionResource", description = "Controller responsible for templates execution.")
public class TemplateExecutionResource {

    private static final String PAYMENT_RECEIVED_EXAMPLE = "{\n" +
            "        \"type\" :\"PAYMENT_RECEIVED\", \n" +
            "        \"accountCode\" : \"DEFAULT\", \n" +
            "        \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
            "        \"additionalData\" : { " +
            "             \"bankAccountCode\" : \"ItalyClients1\", " +
            "             \"discountAmount\" : 10," +
            "             \"description\" : \"Payment from DEFAULT, discount applied\" " +
            "        },\n" +
            "        \"source\" : \"FRONT_END_APP\"\n" +
            "}";

    private static final String PAYMENT_RECEIVED_CORRECTION_EXAMPLE = "{\n" +
            "        \"type\" :\"PAYMENT_RECEIVED_CORRECTION\", \n" +
            "        \"accountCode\" : \"DEFAULT\", \n" +
            "        \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
            "        \"additionalData\" : { \n" +
            "             \"bankAccountCode\" : \"ItalyClients1\",\n" +
            "             \"discountAmount\" : 10,\n" +
            "             \"description\" : \"Correction previous erroneous posting.\"\n" +
            "        },\n" +
            "        \"source\" : \"FRONT_END_APP\"\n" +
            "}";

    private static final String CLIENT_FEE_PAYOUT_EXAMPLE = "{\n" +
            "        \"type\" :\"CLIENT_FEE_PAYOUT\", \n" +
            "        \"accountCode\" : \"DEFAULT\", \n" +
            "        \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
            "        \"source\" : \"FRONT_END_APP\"\n" +
            "}";

    private final TemplateExecutionService templateExecutionService;

    @POST
    @ApiKeyAuth
    @PreProcess(role = SystemConstants.UserRole.SUPER_USER)
    @Path("execute")
    @Operation(summary = "Execute template. JSON should be passed as body.")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.TEXT_PLAIN)
    )
    @RequestBody(
            description = "Ledger JSON message to forward to the processing queue.",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = QueueMessageDTO.class),
                    mediaType = MediaType.APPLICATION_JSON,
                    examples = {
                            @ExampleObject(name="PAYMENT_RECEIVED", description = "Client payment received", value = PAYMENT_RECEIVED_EXAMPLE),
                            @ExampleObject(name="PAYMENT_RECEIVED_CORRECTION", description = "Posts a reverse journal of PAYMENT_RECEIVED", value = PAYMENT_RECEIVED_CORRECTION_EXAMPLE),
                            @ExampleObject(name="CLIENT_FEE_PAYOUT", description = "Client fee payout", value = CLIENT_FEE_PAYOUT_EXAMPLE)
                    }
            ))
    public Response execute(@Context HttpHeaders headers, @NotNull QueueMessageDTO message) {
        templateExecutionService.executeTemplate(message);
        return Response.ok().build();
    }

    @POST
    @ApiKeyAuth
    @PreProcess(role = SystemConstants.UserRole.SUPER_USER)
    @Path("companies/{companyId}/journals")
    @Operation(summary = "Post a custom journal")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.TEXT_PLAIN)
    )
    @RequestBody(
            description = "Journal data",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = JournalPostingDTO.class),
                    mediaType = MediaType.APPLICATION_JSON,
                    examples = {
                            @ExampleObject(
                                    name = "Journal Posting Example",
                                    value = """
                                            {
                                              "bookingDate": "2023-11-15",
                                              "journalLines": [
                                                {
                                                  "registerTypeCode": "BANK",
                                                  "lineType": "DEBIT",
                                                  "amount": 500.00,
                                                  "currencyCode": "EUR"
                                                },
                                                {
                                                  "registerTypeCode": "REVENUE",
                                                  "lineType": "CREDIT",
                                                  "amount": 500.00,
                                                  "currencyCode": "EUR"
                                                }
                                              ]
                                            }"""
                            )
                    }
            ))
    public Response postJournal(@Context HttpHeaders headers, @PathParam("companyId") int companyId, @NotNull JsonNode journalJson) {
        try {
            templateExecutionService.postJournal(companyId, journalJson);
            return Response.ok()
                    .entity("Journal successfully posted")
                    .build();
        } catch (Exception e) {
            log.error("Error processing journal posting", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error processing journal: " + e.getMessage())
                    .build();
        }
    }


}
