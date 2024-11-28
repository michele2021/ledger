package com.accounting.core.admin;

import com.accounting.commons.queue.MessageQueueClient;
import com.accounting.commons.queue.MessagesSkipperService;
import com.accounting.commons.tasks.QueueProcessorTask;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.Configuration;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.accounting.core.testUtils.TestUtils.assertArg;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;



@ExtendWith(MockitoExtension.class)
class AdminResourceBundleTest {



    @Mock
    JerseyEnvironment jerseyEnvironment;

    @Mock
    Bootstrap<?> bootstrap;

    @Mock
    Configuration configuration;

    @Mock
    Environment environment;

    @Mock
    MetricRegistry metricRegistry;

    @Mock
    AdminEnvironment adminEnvironment;

    @Mock
    ServletRegistration.Dynamic servletRegistration;

    @InjectMocks
    AdminResourceBundle subject;

    @Test
    void testInitialize() {
        //given

        //when
        subject.initialize(bootstrap);

        //then
        verifyNoInteractions(jerseyEnvironment, bootstrap, configuration, environment, metricRegistry, adminEnvironment);
    }

    @Test
    void testRun() {
        //given
        when(environment.metrics()).thenReturn(metricRegistry);
        when(environment.admin()).thenReturn(adminEnvironment);

        when(adminEnvironment.addServlet(anyString(), any(Servlet.class))).thenReturn(servletRegistration);

        //when
        subject.run(configuration, environment);

        //then
        assertThat(subject.jersey()).isNotNull();
        assertThat(subject.jersey().getResourceConfig().getContextPath()).isEqualTo("/admin");

        verifyNoInteractions(jerseyEnvironment, bootstrap, configuration);
        verify(environment).metrics();
        verify(environment).admin();
        verifyNoMoreInteractions(environment);
        verify(adminEnvironment).addServlet(eq("admin resources"), assertArg( (ServletContainer container) -> {
            assertThat(container).isInstanceOf(ServletContainer.class);
        }));
        verifyNoMoreInteractions(adminEnvironment);
        verify(servletRegistration).addMapping(eq("/admin/*"));

    }

}