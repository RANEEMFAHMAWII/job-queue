package com.example.jobqueue.dto;

import com.example.jobqueue.entity.JobType;

import java.util.UUID;

public record JobMessage(
        UUID jobId,
        JobType jobType,
        String payload
) {
}
