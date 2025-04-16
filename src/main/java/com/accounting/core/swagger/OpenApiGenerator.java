package com.accounting.core.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OpenApiGenerator {
    public static void main(String[] args) throws Exception {
        // Create a minimal configuration similar to what would happen in the app
        SwaggerConfig swaggerConfig = new SwaggerConfig();

        // Build the OpenAPI configuration
        SwaggerConfiguration config = swaggerConfig.build();

        // Create the context builder
        OpenAPI openAPI = new JaxrsOpenApiContextBuilder<>()
                .openApiConfiguration(config)
                .buildContext(true)
                .read();

        // Save as JSON
        ObjectMapper jsonMapper = Json.mapper();
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String jsonOutput = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);
        Files.writeString(Paths.get("swagger.json"), jsonOutput);


        // Save as YAML
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        yamlMapper.writeValue(new File("swagger.yaml"), openAPI);
        System.out.println("Generated swagger.yaml");
    }
}
