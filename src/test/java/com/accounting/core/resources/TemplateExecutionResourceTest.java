package com.accounting.core.resources;

import com.accounting.commons.SystemConstants;
import com.accounting.commons.resources.security.entities.AuthUser;
import com.accounting.core.dto.QueueMessageAmountDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.services.TemplateExecutionService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static com.accounting.commons.utils.MappingUtils.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TemplateExecutionResourceTest {

    @Mock
    TemplateExecutionService templateExecutionService;

    /**
     * POST /accounting/journals/from-template
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

        AuthUser authUser = new AuthUser(1L, "testUser", 100L, "testAccountCode",
                Set.of(SystemConstants.UserRole.SUPER_USER), Instant.now());

        TemplateExecutionResource resource = new TemplateExecutionResource(templateExecutionService);

        //when
        final Response response = resource.execute(null, authUser, message);

        //then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(templateExecutionService).executeTemplate("testAccountCode", message);
    }
}
