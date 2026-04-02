package com.example.jobqueue.exception;

import java.util.UUID;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(UUID jobId) {
        super("Access denied to job: " + jobId);
    }
}
