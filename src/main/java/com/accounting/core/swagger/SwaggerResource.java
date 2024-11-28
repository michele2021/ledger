package com.accounting.core.swagger;

import com.google.inject.Singleton;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

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
        return Response.seeOther(URI.create(format("/swagger.%s", type))).build();
    }

}
