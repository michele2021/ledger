package com.accounting.core.admin;

import com.accounting.commons.queue.MessageQueueClient;
import com.accounting.core.swagger.ApiDefaultResponses;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.accounting.core.swagger.ApiDocConstants.HTTP_STATUS_OK;
import static com.accounting.core.swagger.ApiDocConstants.OK_STATUS_DESCRIPTION;

@Slf4j
@Singleton
@Path("/queue")
@ApiDefaultResponses
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Tag(name = "QueueAdminResource", description = "Controller responsible for queue health check.")
public class QueueAdminResource {

    private final MessageQueueClient messageQueueClient;
    private final Gson gson = new Gson();

    @GET
    @Path("/health")
    @Server(url = "https://ledger-gateway.ledger.works/ledger-admin/admin", description = "Staging environment")
    @Server(url = "https://ledger-gateway.ledger.link/ledger-admin/admin", description = "Integration environment")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @Operation(summary = "Check the queue status.")
    public Response health() {
        return Response.ok()
                .entity(gson.toJson(messageQueueClient.getStatusData()))
                .build();
    }
}
