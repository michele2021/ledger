package com.accounting.core.swagger;

import com.google.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Singleton
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
public class SwaggerResource {

    private static final String MEDIA_TYPE_APPLICATION_YAML = "application/yaml";

    @GET
    @Produces({MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_YAML})
    @Path("/swagger.{type:json|yaml}")
    public Response redirectSwaggerDoc(@PathParam("type") String type) {
        return Response.seeOther(URI.create("/openapi.%s".formatted(type))).build();
    }
}
