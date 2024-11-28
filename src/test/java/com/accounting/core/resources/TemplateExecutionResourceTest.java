package com.accounting.core.resources;

import com.accounting.core.dto.QueueMessageAmountDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.services.TemplateExecutionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class TemplateExecutionResourceTest {

    TemplateExecutionService templateExecutionService = mock(TemplateExecutionService.class);

    ResourceExtension EXT = ResourceExtension.builder()
            .addResource(new TemplateExecutionResource(templateExecutionService))
            .build();

    ObjectMapper objectMapper = new ObjectMapper();


    /**
     * POST /accounting/template/execute
     */
    @Test
    void executeTemplate() throws Exception {
        //given
        QueueMessageDTO message = QueueMessageDTO.builder()
                .type("type")
                .accountCode("accountCode")
                .amount(QueueMessageAmountDTO.builder()
                        .quantity(new BigDecimal("1234.56"))
                        .currencyCode("ABC")
                        .build())
                .uuid("uuid")
                .txReference("txReference")
                .additionalData(objectMapper.readTree("{ \"test\" : \"value\" }"))
                .source("source")
                .build();

        //when
        final Response response = EXT.target("/accounting/template/execute")
                .request()
                .post(Entity.entity(message, MediaType.APPLICATION_JSON_TYPE));

        //then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(templateExecutionService).executeTemplate(eq(message));
    }
}