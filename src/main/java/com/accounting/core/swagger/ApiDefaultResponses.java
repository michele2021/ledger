package com.accounting.core.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.ws.rs.core.MediaType;
import java.lang.annotation.*;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ApiResponses({
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error or unhandled error",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, examples = @ExampleObject("{\n\"code\": 500,\n\"message\": \"Detailed message.\"\n}"))
        ),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized - missing or invalid API KEY",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, examples = @ExampleObject("{\n\"code\": 401,\n\"message\": \"User must be authenticated to perform this action.\"\n}"))
        )
})
public @interface ApiDefaultResponses {
}
