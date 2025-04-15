package com.accounting.core.services;

import com.accounting.core.dto.QueueMessageDTO;
import com.fasterxml.jackson.databind.JsonNode;

public interface TemplateExecutionService {

    void executeTemplate(QueueMessageDTO message);

    void postJournal(int companyId, JsonNode journalJson);

}
