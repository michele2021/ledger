package com.accounting.core.services;

import com.accounting.core.dto.JournalPostingDTO;
import com.accounting.core.dto.QueueMessageDTO;

public interface TemplateExecutionService {

    void executeTemplate(QueueMessageDTO message);

    void postJournal(int companyId, JournalPostingDTO journal);

}
