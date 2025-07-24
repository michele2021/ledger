package com.accounting.core.services;

import com.accounting.core.dto.JournalPostingDTO;
import com.accounting.core.dto.QueueMessageDTO;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import java.util.List;

public interface TemplateExecutionService {

    void executeTemplate(String accountCode, QueueMessageDTO message);
    void postJournalWithAttachments(long accountId, JournalPostingDTO journalPostingDTO, List<FormDataBodyPart> files);
    void postJournal(long accountId, JournalPostingDTO journalPosting);
}
