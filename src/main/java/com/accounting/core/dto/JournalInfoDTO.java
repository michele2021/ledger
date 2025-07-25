package com.accounting.core.dto;

import lombok.Builder;

@Builder
public record JournalInfoDTO(
        String id,          // UUID from FileStorage
        String type,        // "image", "pdf", "text", …
        String filename,    // original client name
        String contentType, // MIME, e.g. "image/png"
        String textContent, // small text note
        String uri,         // s3://…, pgsql://…, …
        String sha256,      // hex-encoded checksum
        long size           // bytes
) {}