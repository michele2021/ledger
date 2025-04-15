package com.accounting.core.dto;

import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record JournalPostingDTO(
        String bookingDate,
        String description,
        List<JournalLinePostingDTO> journalLines
) { }
