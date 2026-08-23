package com.fileupload.common.exception;

public class PolicyConflictException
    extends RuntimeException {

    public PolicyConflictException() {
        super("Extension policy has already been modified.");
    }
}