package com.accounting.core.swagger;

import com.google.inject.Singleton;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.File;

import static java.lang.String.format;

@Singleton
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
public class SwaggerResource {

    private static final String MEDIA_TYPE_APPLICATION_YAML = "application/yaml";

    @GET
    @Produces({MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_YAML})
    @Path(ApiDocConstants.SWAGGER_URI_PREFIX + "/swagger.{type:json|yaml}")
    public Response redirectSwaggerDoc(@PathParam("type") String type) {
        //return Response.seeOther(URI.create(format("/swagger.%s", type))).build();
        File file = new File(format("./swagger.%s", type));
        return Response.ok(file).build();
    }
}
