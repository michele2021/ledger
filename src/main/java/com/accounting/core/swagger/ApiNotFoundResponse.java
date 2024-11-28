package com.accounting.core.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;


@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content = @Content(examples = @ExampleObject("{\n\"code\": 404,\n\"message\": \"Detailed message.\"\n}"))
)
public @interface ApiNotFoundResponse {
}
