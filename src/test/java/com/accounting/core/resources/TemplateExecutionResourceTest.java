package com.accounting.core.resources;

import com.accounting.core.dto.QueueMessageAmountDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.services.TemplateExecutionService;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static com.accounting.commons.utils.MappingUtils.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class TemplateExecutionResourceTest {

    TemplateExecutionService templateExecutionService = mock(TemplateExecutionService.class);

    ResourceExtension EXT = ResourceExtension.builder()
            .addResource(new TemplateExecutionResource(templateExecutionService))
            .build();


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
                .additionalData(OBJECT_MAPPER.readTree("{ \"test\" : \"value\" }"))
                .source("source")
                .build();

        //when
        final Response response = EXT.target("/accounting/template/execute")
                .request()
                .post(Entity.entity(message, MediaType.APPLICATION_JSON_TYPE));

        //then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(templateExecutionService).executeTemplate(null, eq(message));
    }
}