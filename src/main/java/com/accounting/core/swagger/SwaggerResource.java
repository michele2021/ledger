package com.accounting.core.swagger;

import com.google.inject.Singleton;

import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Singleton
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
public class SwaggerResource {

    private static final String MEDIA_TYPE_APPLICATION_YAML = "application/yaml";

    @GET
    @Produces({MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_YAML})
    @Path("/swagger.{type:json|yaml}")
    public Response redirectSwaggerDoc(@PathParam("type") String type) throws OpenApiConfigurationException, IOException {
        if (StringUtils.isEmpty(type)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var openAPI = OpenApiGenerator.getOpenAPI();
        if (type.equals("json")) {
            return Response.ok(OpenApiGenerator.getSwaggerJsonOutput(openAPI)).build();
        } else if (type.equals("yaml")) {
            return Response.ok(OpenApiGenerator.getSwaggerYamlOutput(openAPI)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}
