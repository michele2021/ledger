package com.accounting.core.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OpenApiGenerator {
    public static void main(String[] args) throws Exception {
        OpenAPI openAPI = getOpenAPI();

        // Save as JSON
        String jsonOutput = getSwaggerJsonOutput(openAPI);
        Files.writeString(Paths.get("swagger.json"), jsonOutput);
        System.out.println("Generated swagger.json");

        // Save as YAML
        String yamlOutput = getSwaggerYamlOutput(openAPI);
        Files.writeString(Paths.get("swagger.yaml"), yamlOutput);
        System.out.println("Generated swagger.yaml");
    }

    private static OpenAPI getOpenAPI() throws OpenApiConfigurationException {
        // Create a minimal configuration similar to what would happen in the app
        SwaggerConfig swaggerConfig = new SwaggerConfig();

        // Build the OpenAPI configuration
        SwaggerConfiguration config = swaggerConfig.build();

        // Create the context builder
        return new JaxrsOpenApiContextBuilder<>()
                .openApiConfiguration(config)
                .buildContext(true)
                .read();
    }

    private static String getSwaggerYamlOutput(OpenAPI openAPI) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return yamlMapper.writeValueAsString(openAPI);

    }

    private static String getSwaggerJsonOutput(OpenAPI openAPI) throws IOException {
        ObjectMapper jsonMapper = Json.mapper();
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);
    }
}
