package com.fileupload.common.exception;

public class CustomExtensionLimitException extends RuntimeException {

    public CustomExtensionLimitException() {
        super("Custom extension limit reached.");
    }
}