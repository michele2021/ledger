package com.accounting.core.resources;

import com.accounting.commons.SystemConstants;
import com.accounting.commons.resources.security.ApiKeyAuth;
import com.accounting.commons.resources.security.PreProcess;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.services.TemplateExecutionService;
import com.accounting.core.swagger.ApiBadRequestResponse;
import com.accounting.core.swagger.ApiDefaultResponses;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.accounting.core.swagger.ApiDocConstants.HTTP_STATUS_OK;
import static com.accounting.core.swagger.ApiDocConstants.OK_STATUS_DESCRIPTION;

@Slf4j
@Singleton
@Path("/accounting/template")
@ApiDefaultResponses
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Tag(name = "TemplateExecutionResource", description = "Controller responsible for templates execution.")
public class TemplateExecutionResource {

    private static final String PAYMENT_RECEIVED_EXAMPLE = "{\n" +
            "        \"type\" :\"PAYMENT_RECEIVED\", \n" +
            "        \"accountCode\" : \"EPAY_IT\", \n" +
            "        \"amount\" : { \"quantity\":150.00, \"currencyCode\":\"EUR\" },\n" +
            "        \"additionalData\" : { " +
            "             \"bankAccountCode\" : \"ItalyClients1\", " +
            "             \"discountAmount\" : 10," +
            "             \"description\" : \"Payment from EPAY_IT, discount applied\" " +
            "        },\n" +
            "        \"source\" : \"FRONT_END_APP\"\n" +
            "}";

    private static final String PAYMENT_RECEIVED_CORRECTION_EXAMPLE = "{\n" +
            "        \"type\" :\"PAYMENT_RECEIVED_CORRECTION\", \n" +
            "        \"accountCode\" : \"EPAY_IT\", \n" +
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
            "        \"accountCode\" : \"EPAY_IT\", \n" +
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
    public Response executeTemplate(@Context HttpHeaders headers, @NotNull QueueMessageDTO message) {
        templateExecutionService.executeTemplate(message);
        return Response.ok().build();
    }

}
