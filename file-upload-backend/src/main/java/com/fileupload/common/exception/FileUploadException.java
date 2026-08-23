package com.fileupload.common.exception;

import org.springframework.http.HttpStatus;

public class FileUploadException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String userMessage;

    public FileUploadException(
        HttpStatus status,
        String code,
        String userMessage
    ) {
        super(code);
        this.status = status;
        this.code = code;
        this.userMessage = userMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getUserMessage() {
        return userMessage;
    }
}