package com.accounting.core.admin;

import com.accounting.commons.queue.MessageQueueClient;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(DropwizardExtensionsSupport.class)
class QueueAdminResourceTest {

    MessageQueueClient messageQueueClient = mock(MessageQueueClient.class);

    ResourceExtension EXT = ResourceExtension.builder()
            .addResource(new QueueAdminResource(messageQueueClient))
            .build();

    /**
     * GET [/admin]/queue/health -> 200
     */
    @Test
    void executeHealthCheck() {
        //given
        Map<String, Object> statusData = new LinkedHashMap<>();
        statusData.put("isRunning", true);
        statusData.put("messageId", "messageId value");
        statusData.put("exception", "exception value");

        when(messageQueueClient.getStatusData()).thenReturn(statusData);

        //when
        final Response response = EXT.target("/queue/health")
                .request()
                .get();

        //then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(String.class)).isEqualTo("{\"isRunning\":true,\"messageId\":\"messageId value\",\"exception\":\"exception value\"}");
    }

}