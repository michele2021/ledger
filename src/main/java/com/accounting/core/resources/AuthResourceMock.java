package com.accounting.core.resources;

import com.accounting.commons.resources.security.ApiKeyAuth;
import com.accounting.commons.utils.SerializationUtil;
import com.accounting.core.services.security.AuthServiceMock;
import com.accounting.core.swagger.ApiBadRequestResponse;
import com.accounting.core.swagger.ApiDefaultResponses;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import static com.accounting.core.swagger.ApiDocConstants.HTTP_STATUS_OK;
import static com.accounting.core.swagger.ApiDocConstants.OK_STATUS_DESCRIPTION;

@Slf4j
@Singleton
@Path("/auth")
@ApiDefaultResponses
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Tag(name = "MockLoginResource", description = "Controller responsible for mocking an authentication service.")
public class AuthResourceMock {
    private final AuthServiceMock authServiceMock;
    private final Gson gson = SerializationUtil.getGson();

    @POST
    @ApiKeyAuth
    @Path("login")
    @Operation(summary = "Simulates a successful login.")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.TEXT_PLAIN)
    )
    @RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = JsonNode.class),
                    mediaType = MediaType.APPLICATION_JSON
            ))
    public Response login(@Context HttpHeaders headers, @NotNull JsonNode jsonNode) {
        log.info("Login headers: {}, jsonNode {}", headers, jsonNode);
        var username = jsonNode.get("username").asText();
        var authResult = authServiceMock.login(username);
        return Response
                .ok()
                .entity(gson.toJson(authResult))
                .build();
    }

    @POST
    @ApiKeyAuth
    @Path("refresh-token")
    @Operation(summary = "Simulates a successful token refresh.")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.TEXT_PLAIN)
    )
    @RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = JsonNode.class),
                    mediaType = MediaType.APPLICATION_JSON
            ))
    public Response refreshToken(@Context HttpHeaders headers, @NotNull JsonNode jsonNode) {
        // TODO get the refresh token from the jsonNode payload
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        var authResult = authServiceMock.refreshToken(authHeader);
        if (authResult == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    //.header("WWW-Authenticate", "Bearer realm=\"ledger\"")
                    .entity("Refresh token is missing or invalid.")
                    .build();
        }
        return Response
                .ok()
                .entity(gson.toJson(authResult))
                .build();
    }

    @POST
    @ApiKeyAuth
    @Path("revoke-token")
    @Operation(summary = "Simulates a successful token revocation.")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.TEXT_PLAIN)
    )
    @RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = JsonNode.class),
                    mediaType = MediaType.APPLICATION_JSON
            ))
    public Response revokeToken(@Context HttpHeaders headers, @NotNull JsonNode jsonNode) {
        authServiceMock.revokeToken();
        return Response
                .ok()
                .build();
    }

    @POST
    @ApiKeyAuth
    @Path("change-password")
    @Operation(summary = "Simulates a successful password change.")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.TEXT_PLAIN)
    )
    @RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = JsonNode.class),
                    mediaType = MediaType.APPLICATION_JSON
            ))
    public Response changePassword(@Context HttpHeaders headers, @NotNull JsonNode jsonNode) {
        authServiceMock.changePassword();
        return Response
                .ok()
                .build();
    }
}