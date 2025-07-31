package com.accounting.core.dto;

import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder
public record JournalInfoDTO(
        UUID id,            // UUID from FileStorage
        String type,        // "image", "pdf", "text", …
        String filename,    // original client name
        String contentType, // MIME, e.g. "image/png"
        String textContent,// small text note, currently not used since all the notes are stored in the "object" schema.
        String uri,         // s3://…, pgsql://…, …
        String sha256,      // hex-encoded checksum
        long size           // bytes
) {

    // Compact constructor for validation
    public JournalInfoDTO {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(contentType, "contentType cannot be null");
        Objects.requireNonNull(uri, "uri cannot be null");
        Objects.requireNonNull(sha256, "sha256 cannot be null");
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative");
        }
    }

}