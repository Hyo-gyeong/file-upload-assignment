package com.fileupload.auth.dto;

public record AuthErrorResponse(
    String code,
    String message
) {
}