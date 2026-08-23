package com.fileupload.common.exception;

public class DuplicateExtensionException extends RuntimeException {

    public DuplicateExtensionException() {
        super("Extension already exists.");
    }
}