package com.accounting.core.resources;

import com.accounting.commons.queue.MessagesSkipperService;
import com.accounting.commons.resources.security.ApiKeyAuth;
import com.accounting.core.swagger.ApiBadRequestResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static com.accounting.core.swagger.ApiDocConstants.HTTP_STATUS_OK;
import static com.accounting.core.swagger.ApiDocConstants.OK_STATUS_DESCRIPTION;

@Slf4j
@Singleton
@Path("/accounting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Tag(name = "AccountingQueuesResource", description = "Controller responsible for queue messages management.")
public class AccountingQueuesResource {

    private final MessagesSkipperService messagesSkipperService;

    /*
     * If the message is present in the AMQ skip list, marks the entry as skipped.
     */
    @POST
    @ApiKeyAuth
    @Path("amqueue/{messageId}/skip")
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(schema = @Schema(), mediaType = MediaType.APPLICATION_JSON)
    )
    @Operation(summary = "Skip message in the AMQ. MessageId should be passed as path param.")
    public Response skipAMQMessage(@Context HttpHeaders headers, @PathParam("messageId") String messageId) {
        log.info("Received skip AMQ message for message id: {}", messageId);
        boolean ret = messagesSkipperService.skipAMQMessage(messageId);
        String jsonEntity = "{\"result\" : " + ret + "}";
        return Response.ok()
                .entity(jsonEntity)
                .build();
    }

    /*
     * Creates an entry in the Accounting SQS skip list.
     */
    @PUT
    @ApiKeyAuth
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(schema = @Schema(), mediaType = MediaType.APPLICATION_JSON)
    )
    @ApiResponse(
            responseCode = "409",
            description = "Conflict",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, examples = @ExampleObject("{\n\"code\": 409,\n\"message\": \"Detailed message.\"\n}"))
    )
    @Path("queue/skip/{messageId}/entry")
    @Operation(summary = "Skip message in SQS. MessageId should be passed as path param.")
    public Response skipMessage(@Context HttpHeaders headers, @PathParam("messageId") String messageId) {
        log.info("Received message to add to the skip list for the accounting queue. Message id: {}", messageId);
        try {
            messagesSkipperService.addSkipEntry(messageId);
        } catch (UnableToExecuteStatementException e) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        return Response.status(Response.Status.CREATED).build();
    }
}
