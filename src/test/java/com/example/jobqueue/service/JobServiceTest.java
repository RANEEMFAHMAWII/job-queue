package com.example.jobqueue.service;

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
import com.example.jobqueue.worker.ReverseStringWorker;
import com.example.jobqueue.worker.WordCountWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobProducer jobProducer;

    private JobService jobService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jobService = new JobService(
                jobRepository,
                jobProducer,
                new WordCountWorker(),
                new ReverseStringWorker(),
                3
        );
    }

    @Test
    void submitJob_shouldPersistAndPublish() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobSubmitRequest request = new JobSubmitRequest(JobType.WORD_COUNT, "hello world");
        JobSubmitResponse response = jobService.submitJob(userId, request);

        assertNotNull(response.jobId());
        assertEquals(JobStatus.PENDING, response.status());

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        verify(jobProducer).send(any());
    }

    @Test
    void getJobStatus_shouldReturnJobForOwner() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job(jobId, userId, JobType.WORD_COUNT, "hello world");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobStatusResponse response = jobService.getJobStatus(userId, jobId);

        assertEquals(jobId, response.jobId());
        assertEquals(JobType.WORD_COUNT, response.type());
        assertEquals("hello world", response.payload());
        assertEquals(JobStatus.PENDING, response.status());
    }

    @Test
    void getJobStatus_shouldThrowWhenNotFound() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getJobStatus(userId, jobId));
    }

    @Test
    void getJobStatus_shouldThrowWhenNotOwner() {
        UUID jobId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        Job job = new Job(jobId, otherUser, JobType.WORD_COUNT, "text");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThrows(AccessDeniedException.class, () -> jobService.getJobStatus(userId, jobId));
    }

    @Test
    void processJob_wordCount_shouldComplete() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job(jobId, userId, JobType.WORD_COUNT, "one two three");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        jobService.processJob(jobId, JobType.WORD_COUNT, "one two three");

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeast(2)).save(captor.capture());

        Job saved = captor.getAllValues().getLast();
        assertEquals(JobStatus.COMPLETED, saved.getStatus());
        assertEquals("3", saved.getResult());
    }

    @Test
    void processJob_reverseString_shouldComplete() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job(jobId, userId, JobType.REVERSE_STRING, "hello");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        jobService.processJob(jobId, JobType.REVERSE_STRING, "hello");

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeast(2)).save(captor.capture());

        Job saved = captor.getAllValues().getLast();
        assertEquals(JobStatus.COMPLETED, saved.getStatus());
        assertEquals("olleh", saved.getResult());
    }

    @Test
    void processJob_shouldSkipUnknownJob() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        jobService.processJob(jobId, JobType.WORD_COUNT, "text");

        verify(jobRepository, never()).save(any());
    }

    @Test
    void processJob_shouldSkipAlreadyCompletedJob() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job(jobId, userId, JobType.WORD_COUNT, "text");
        job.setStatus(JobStatus.COMPLETED);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        jobService.processJob(jobId, JobType.WORD_COUNT, "text");

        verify(jobRepository, never()).save(any());
    }
}
