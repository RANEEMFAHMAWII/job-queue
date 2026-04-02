package com.example.jobqueue.dto;

import com.example.jobqueue.entity.JobStatus;
import com.example.jobqueue.entity.JobType;

import java.time.Instant;
import java.util.UUID;

public record JobStatusResponse(
        UUID jobId,
        JobType type,
        String payload,
        String result,
        JobStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
