package com.accounting.core.resources;

import com.accounting.commons.resources.security.ApiKeyAuth;
import com.accounting.core.dto.AccountDTO;
import com.accounting.core.services.AccountService;
import com.accounting.core.swagger.ApiBadRequestResponse;
import com.accounting.core.swagger.ApiDefaultResponses;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Path("/accounting/account")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@ApiDefaultResponses
@Tag(name = "AccountResource", description = "Controller responsible for account (write) operations.")
public class AccountResource {

    private final AccountService accountService;

    private static final String CMPZERO_ACCOUNT_EXAMPLE = """
            {
                    "accountCode" :"DEFAULT",\s
                    "accountTypeCode" : "Client",\s
                    "description" : "https://www.default.it/"
            }""";

    @POST
    @ApiKeyAuth
    @Path("create")
    @Operation(summary = "Create an account.")
    @ApiBadRequestResponse
    @ApiResponse(
            responseCode = HTTP_STATUS_OK,
            description = OK_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.TEXT_PLAIN)
    )
    @RequestBody(
            description = "Creation of an account. Optionally add account properties and client details. Optionally create a corresponding test account. Optionally request the opening of a corresponding Virtual Iban (with Monavate).",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = AccountDTO.class),
                    examples = {
                            @ExampleObject(name="DEFAULT", description = "Creation of DEFAULT Company account", value = CMPZERO_ACCOUNT_EXAMPLE)
                    }
            ))
    public Response create(@Context HttpHeaders headers, @NotNull @Valid AccountDTO accountDTO) {
        long accountId = accountService.createAccount(accountDTO);
        JsonObject jo = new Gson().fromJson("{ \"accountId\" : " + accountId + " }", JsonObject.class);
        return Response.ok()
                .entity(jo.toString())
                .build();
    }
}
