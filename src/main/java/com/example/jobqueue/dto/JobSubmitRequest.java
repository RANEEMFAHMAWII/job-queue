package com.example.jobqueue.dto;

import com.example.jobqueue.entity.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobSubmitRequest(
        @NotNull(message = "Job type is required")
        JobType type,

        @NotBlank(message = "Payload must not be blank")
        String payload
) {
}
