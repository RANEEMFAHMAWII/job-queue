package com.example.jobqueue.service;

import com.example.jobqueue.dto.JobMessage;
import com.example.jobqueue.dto.JobStatusResponse;
import com.example.jobqueue.dto.JobSubmitRequest;
import com.example.jobqueue.dto.JobSubmitResponse;
import com.example.jobqueue.entity.Job;
import com.example.jobqueue.entity.JobStatus;
import com.example.jobqueue.entity.JobType;
import com.example.jobqueue.exception.AccessDeniedException;
import com.example.jobqueue.exception.JobNotFoundException;
import com.example.jobqueue.producer.JobProducer;
import com.example.jobqueue.repository.JobRepository;
import com.example.jobqueue.worker.JobWorker;
import com.example.jobqueue.worker.ReverseStringWorker;
import com.example.jobqueue.worker.WordCountWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final JobProducer jobProducer;
    private final WordCountWorker wordCountWorker;
    private final ReverseStringWorker reverseStringWorker;
    private final int maxRetries;

    public JobService(JobRepository jobRepository,
                      JobProducer jobProducer,
                      WordCountWorker wordCountWorker,
                      ReverseStringWorker reverseStringWorker,
                      @Value("${app.job.max-retries}") int maxRetries) {
        this.jobRepository = jobRepository;
        this.jobProducer = jobProducer;
        this.wordCountWorker = wordCountWorker;
        this.reverseStringWorker = reverseStringWorker;
        this.maxRetries = maxRetries;
    }

    @Transactional
    public JobSubmitResponse submitJob(UUID userId, JobSubmitRequest request) {
        UUID jobId = UUID.randomUUID();
        Job job = new Job(jobId, userId, request.type(), request.payload());
        jobRepository.save(job);

        JobMessage message = new JobMessage(jobId, request.type(), request.payload());
        jobProducer.send(message);

        log.info("Job submitted: id={}, type={}, user={}", jobId, request.type(), userId);
        return new JobSubmitResponse(jobId, job.getStatus());
    }

    @Transactional(readOnly = true)
    public JobStatusResponse getJobStatus(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!userId.equals(job.getUserId())) {
            throw new AccessDeniedException(jobId);
        }

        return toResponse(job);
    }

    @Transactional
    public void processJob(UUID jobId, JobType jobType, String payload) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Received message for unknown job: {}", jobId);
            return;
        }

        if (job.getStatus() != JobStatus.PENDING && job.getStatus() != JobStatus.RUNNING) {
            log.info("Skipping job {} — already in status {}", jobId, job.getStatus());
            return;
        }

        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);

        try {
            JobWorker worker = resolveWorker(jobType);
            String result = worker.process(payload);

            job.setResult(result);
            job.setStatus(JobStatus.COMPLETED);
            jobRepository.save(job);
            log.info("Job completed: id={}, result={}", jobId, result);

        } catch (Exception ex) {
            handleFailure(job, ex);
        }
    }

    private void handleFailure(Job job, Exception ex) {
        int attempt = job.getRetries() + 1;
        job.setRetries(attempt);

        if (attempt >= maxRetries) {
            job.setStatus(JobStatus.FAILED);
            log.error("Job {} failed permanently after {} retries", job.getId(), attempt, ex);
        } else {
            job.setStatus(JobStatus.PENDING);
            log.warn("Job {} failed (attempt {}), will retry", job.getId(), attempt, ex);
        }

        jobRepository.save(job);

        if (attempt < maxRetries) {
            throw new RuntimeException("Retriable failure for job " + job.getId(), ex);
        }
    }

    private JobWorker resolveWorker(JobType type) {
        return switch (type) {
            case WORD_COUNT -> wordCountWorker;
            case REVERSE_STRING -> reverseStringWorker;
        };
    }

    private JobStatusResponse toResponse(Job job) {
        return new JobStatusResponse(
                job.getId(),
                job.getType(),
                job.getPayload(),
                job.getResult(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
