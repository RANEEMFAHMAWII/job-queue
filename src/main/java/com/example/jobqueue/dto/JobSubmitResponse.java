package com.example.jobqueue.dto;

import com.example.jobqueue.entity.JobStatus;

import java.util.UUID;

public record JobSubmitResponse(
        UUID jobId,
        JobStatus status
) {
}
