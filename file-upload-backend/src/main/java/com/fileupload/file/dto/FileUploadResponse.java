package com.fileupload.file.dto;

import java.util.UUID;

public record FileUploadResponse(
    UUID id,
    String originalFilename,
    String extension,
    String detectedMimeType,
    long sizeBytes,
    String sha256,
    String status
) {
}