package com.accounting.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record JournalPostingDTO(
        @NotBlank String bookingDate,
        String description,
        List<JournalLinePostingDTO> journalLines
) { }
