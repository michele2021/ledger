package com.accounting.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "Data transfer object used to post a journal")
public record JournalPostingDTO(
        @NotNull
        @Schema(description = "Booking date of the journal", required = true)
        String bookingDate,

        @Schema(description = "Description of the journal", required = true)
        String description,

        @Valid
        @NotEmpty
        @Schema(description = "List of journal lines", required = true)
        List<JournalLinePostingDTO> journalLines
) {
}
