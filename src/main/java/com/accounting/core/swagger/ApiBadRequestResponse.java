package com.accounting.core.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.ws.rs.core.MediaType;
import java.lang.annotation.*;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, examples = @ExampleObject("{\n\"code\": 400,\n\"message\": \"Detailed message.\"\n}"))
)
public @interface ApiBadRequestResponse {
}
