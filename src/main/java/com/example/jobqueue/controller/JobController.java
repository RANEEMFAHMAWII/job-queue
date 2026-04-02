package com.example.jobqueue.controller;

import com.example.jobqueue.dto.JobStatusResponse;
import com.example.jobqueue.dto.JobSubmitRequest;
import com.example.jobqueue.dto.JobSubmitResponse;
import com.example.jobqueue.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobSubmitResponse submitJob(@AuthenticationPrincipal UUID userId,
                                       @Valid @RequestBody JobSubmitRequest request) {
        return jobService.submitJob(userId, request);
    }

    @GetMapping("/{jobId}")
    public JobStatusResponse getJobStatus(@AuthenticationPrincipal UUID userId,
                                          @PathVariable UUID jobId) {
        return jobService.getJobStatus(userId, jobId);
    }
}
