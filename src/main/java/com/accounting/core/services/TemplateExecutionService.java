package com.accounting.core.services;

import com.accounting.core.dto.QueueMessageDTO;
import com.fasterxml.jackson.databind.JsonNode;

public interface TemplateExecutionService {

    void executeTemplate(String accountCode, QueueMessageDTO message);
    void postJournal(long accountId, JsonNode journalJson);
}
