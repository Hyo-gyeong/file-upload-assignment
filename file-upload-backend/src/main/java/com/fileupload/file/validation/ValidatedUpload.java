package com.fileupload.file.validation;

public record ValidatedUpload(
    String originalFilename,
    String extension,
    String clientMimeType,
    String detectedMimeType,
    long sizeBytes
) {
}