package com.fileupload.common.exception;

public record ApiErrorResponse(
    String code,
    String message
) {
}