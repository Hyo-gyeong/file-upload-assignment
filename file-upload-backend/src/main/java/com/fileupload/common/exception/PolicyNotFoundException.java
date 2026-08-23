package com.fileupload.common.exception;

public class PolicyNotFoundException
    extends RuntimeException {

    public PolicyNotFoundException() {
        super("Extension policy was not found.");
    }
}