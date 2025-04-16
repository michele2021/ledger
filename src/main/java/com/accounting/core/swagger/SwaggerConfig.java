package com.accounting.core.swagger;

import com.accounting.commons.CCHTTPHeaders;
import com.accounting.core.config.AccountingCoreAppConfig;
import io.dropwizard.core.setup.Bootstrap;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.accounting.core.swagger.ApiDocConstants.*;
import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

public final class SwaggerConfig extends SwaggerBundleConfiguration {

    private static final String STAGING_ENV = "STAGING";
    private static final String PRODUCTION_ENV = "PRODUCTION";
    private static final String INTEGRATION_ENV = "INTEGRATION";
    private static final String CURRENT_ENVIRONMENT = System.getenv("ENVIRONMENT");
    private static final String LEDGER_SERVICE_HTTP_APP = "LEDGER_SERVICE_HTTP_APP";

    public static void addSwaggerBundle(Bootstrap<AccountingCoreAppConfig> bootstrap) {
        if (!PRODUCTION_ENV.equalsIgnoreCase(CURRENT_ENVIRONMENT)) {
            var swaggerBundle = new SwaggerBundle<AccountingCoreAppConfig>() {
                @Override
                protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(AccountingCoreAppConfig configuration) {
                    var swaggerConfig = configuration.getSwaggerConfig();
                    swaggerConfig.setUriPrefix(StringUtils.EMPTY);
                    return swaggerConfig;
                }
            };
            bootstrap.addBundle(swaggerBundle);
        }
    }

    @Override
    public SwaggerConfiguration build() {

        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList(API_KEY, new ArrayList<>());
        securityRequirement.addList(JWT_TOKEN, new ArrayList<>());

        SecurityScheme apiKeySecurityScheme = new SecurityScheme()
                .name(CCHTTPHeaders.API_KEY)
                .in(SecurityScheme.In.HEADER)
                .type(SecurityScheme.Type.APIKEY);

        SecurityScheme jwtSecurityScheme = new SecurityScheme()
                .name(CCHTTPHeaders.AUTHORIZATION)
                .in(SecurityScheme.In.HEADER)
                .type(SecurityScheme.Type.APIKEY);

        OpenAPI oas = new OpenAPI().info(getInfo())
                .servers(getServers1());

        oas.addSecurityItem(securityRequirement);
        oas.setComponents(new Components());
        oas.getComponents().addSecuritySchemes(API_KEY, apiKeySecurityScheme);
        oas.getComponents().addSecuritySchemes(JWT_TOKEN, jwtSecurityScheme);

        Set<String> exclusions = Set.of(SWAGGER_ENDPOINT);
        return new SwaggerConfiguration().openAPI(oas)
                .prettyPrint(true)
                .readAllResources(true)
                .ignoredRoutes(exclusions)
                .resourcePackages(RESOURCE_PACKAGES);
    }

    private Info getInfo() {
        return new Info()
                .title("Ledger")
                .contact(getContactInfo())
                .description("Ledger execution API.")
                .version("1.0.0");
    }

    private Contact getContactInfo() {
        return new Contact().name("Ledger")
                .email("help@example.com")
                .url("https://example.com");
    }

    private List<Server> getServers1() {
        Server staging = new Server()
                .url("https://ledger-gateway.ledger.works/ledger")
                .description("Staging environment");

        if (STAGING_ENV.equalsIgnoreCase(CURRENT_ENVIRONMENT)) {
            return List.of(staging);
        }

        Server integration = new Server()
                .url("https://ledger-gateway.ledger.link/ledger")
                .description("Integration environment");

        if (INTEGRATION_ENV.equalsIgnoreCase(CURRENT_ENVIRONMENT)) {
            return List.of(integration);
        }

        String port = System.getenv(LEDGER_SERVICE_HTTP_APP);
        Server local = new Server()
                .url(format("http://localhost:%s", isEmpty(port) ? "8080" : port))
                .description("Local development environment");

        return List.of(local, integration, staging);
    }
}
