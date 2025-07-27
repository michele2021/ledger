package com.accounting.core.resources;

import com.accounting.commons.SystemConstants;
import com.accounting.commons.resources.security.ApiKeyAuth;
import com.accounting.commons.resources.security.PreProcess;
import com.accounting.commons.resources.security.entities.AuthUser;
import com.accounting.commons.utils.MappingUtils;
import com.accounting.core.dto.JournalPostingDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.services.TemplateExecutionService;
import com.accounting.core.swagger.ApiBadRequestResponse;
import com.accounting.core.swagger.ApiDefaultResponses;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.accounting.core.swagger.ApiDocConstants.HTTP_STATUS_OK;
import static com.accounting.core.swagger.ApiDocConstants.OK_STATUS_DESCRIPTION;

@Slf4j
@Singleton
@Path("/accounting")
@ApiDefaultResponses
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Tag(name = "TemplateExecutionResource", description = "Controller responsible for templates execution.")
public class TemplateExecutionResource {

    private static final String PAYMENT_RECEIVED_EXAMPLE = """
            {
                    "type" :"PAYMENT_RECEIVED",\s
                    "accountCode" : "DEFAULT",\s
                    "amount" : { "quantity":150.00, "currencyCode":"EUR" },
                    "additionalData" : { \
                         "bankAccountCode" : "ItalyClients1", \
                         "discountAmount" : 10,\
                         "description" : "Payment from DEFAULT, discount applied" \
                    },
                    "source" : "FRONT_END_APP"
            }""";

    private static final String PAYMENT_RECEIVED_CORRECTION_EXAMPLE = """
            {
                    "type" :"PAYMENT_RECEIVED_CORRECTION",\s
                    "accountCode" : "DEFAULT",\s
                    "amount" : { "quantity":150.00, "currencyCode":"EUR" },
                    "additionalData" : {\s
                         "bankAccountCode" : "ItalyClients1",
                         "discountAmount" : 10,
                         "description" : "Correction previous erroneous posting."
                    },
                    "source" : "FRONT_END_APP"
            }""";

    private static final String CLIENT_FEE_PAYOUT_EXAMPLE = """
            {
                    "type" :"CLIENT_FEE_PAYOUT",\s
                    "accountCode" : "DEFAULT",\s
                    "amount" : { "quantity":150.00, "currencyCode":"EUR" },
                    "source" : "FRONT_END_APP"
            }""";

    private final TemplateExecutionService templateExecutionService;

    @POST
    @ApiKeyAuth
    @PreProcess(role = SystemConstants.UserRole.SUPER_USER)
    @Path("journals/from-template")
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
                            @ExampleObject(name = "PAYMENT_MATCH", description = "Match an order and a payment", value = PAYMENT_RECEIVED_EXAMPLE),
                            @ExampleObject(name = "PAYMENT_RECEIVED_CORRECTION", description = "Posts a reverse journal of PAYMENT_RECEIVED", value = PAYMENT_RECEIVED_CORRECTION_EXAMPLE),
                            @ExampleObject(name = "CLIENT_FEE_PAYOUT", description = "Client fee payout", value = CLIENT_FEE_PAYOUT_EXAMPLE)
                    }
            ))
    public Response execute(@Context HttpHeaders headers, @Auth AuthUser authUser, @NotNull QueueMessageDTO message) {
        templateExecutionService.executeTemplate(authUser.companyAccountCode(), message);
        return Response.ok().build();
    }

//    @POST
//    @Path("execute-multipart")
//    @Operation(summary = "Execute template with optional attachments")
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    @Produces(MediaType.APPLICATION_JSON)
//    @PreProcess(role = SystemConstants.UserRole.SUPER_USER)
//    public Response executeMultipart(
//            @Context HttpHeaders headers,
//            @Auth AuthUser user,
//            @FormDataParam("message") QueueMessageDTO queueMessageDTO,
//            @FormDataParam("files") List<FormDataBodyPart> files) {
//        templateExecutionService.postJournalWithAttachments(queueMessageDTO, files, user);
//        return Response.ok().build();
//    }

    // @PreProcess(role = SystemConstants.UserRole.SUPER_USER)
    @POST
    @ApiKeyAuth
    @Path("journals")
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
    public Response postJournal(@Context HttpHeaders headers, @Auth AuthUser user, @NotNull JournalPostingDTO journalPosting) {
        try {
            templateExecutionService.postJournal(user.companyAccountId(), journalPosting);
            return Response.ok()
                    .build();
        } catch (Exception e) {
            log.error("Error processing journal posting", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error processing journal: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("journals/wa")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Journal posted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid form data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreProcess(role = SystemConstants.UserRole.SUPER_USER)
    // TODO to improve scalability (in case of frequent and heavy attachments), move this API to a separate server and/or split the API
    // by having a dedicated endpoint just for attachments and the other endpoint to post just the journal.
    public Response postJournalWithInfo(
            @Parameter(hidden = true) @Context HttpHeaders headers,
            @Parameter(hidden = true) @Auth AuthUser user,
            @Parameter(
                    name        = "journal",
                    description = "Journal data (JSON)",
                    required    = true,
                    content     = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema    = @Schema(implementation = JournalPostingDTO.class)
                    )
            )
            @FormDataParam("journal") InputStream journalJson,
            @Parameter(
                    name        = "files",
                    description = "Attachment files (optional)",
                    content     = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
                    )
            )
            @FormDataParam("files") List<FormDataBodyPart> files
            ) throws IOException {

        // TODO handle IOException from readJournal
        var j = MappingUtils.OBJECT_MAPPER.readValue(journalJson, JournalPostingDTO.class);
        templateExecutionService.postJournalWithInfo(user.companyAccountId(), j, files);
        return Response.ok().build();
    }

//    public static class JournalUploadForm {
//
//        @FormDataParam("journal")               // ← JSON part (content-type: application/json)
//        private InputStream journalJson;
//
//        @FormDataParam("files")                 // ← binary parts
//        private List<FormDataBodyPart> files;
//
//        /** Helper: turn the JSON stream into the DTO with Dropwizard's ObjectMapper */
//        public JournalPostingDTO readJournal(ObjectMapper mapper) throws IOException {
//            return mapper.readValue(journalJson, JournalPostingDTO.class);
//        }
//
//        public List<FormDataBodyPart> files() {
//            return files;
//        }
//    }
}