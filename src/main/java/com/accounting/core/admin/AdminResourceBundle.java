package com.accounting.core.admin;


import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Bundle that gives the ability to register resources under the admin port, the /admin path.
 * Example: Resource with path /queue/health will be available at http://localhost:8081/admin/queue/health
 */
public class AdminResourceBundle implements ConfiguredBundle<Configuration> {

    private JerseyEnvironment jerseyEnvironment;

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
    }

    public JerseyEnvironment jersey() {
        return jerseyEnvironment;
    }

    @Override
    public void run(Configuration configuration, Environment environment) {
        final DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(environment.metrics());
        jerseyConfig.setContextPath("/admin");
        final JerseyContainerHolder servletContainer = new JerseyContainerHolder(new ServletContainer(jerseyConfig));
        jerseyEnvironment = new JerseyEnvironment(servletContainer, jerseyConfig);

        environment.admin()
                .addServlet("admin resources", servletContainer.getContainer())
                .addMapping("/admin/*");
    }

}