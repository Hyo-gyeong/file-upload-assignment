package com.fileupload.common.exception;

public class InvalidExtensionException extends RuntimeException {

    public InvalidExtensionException() {
        super("Invalid extension.");
    }
}