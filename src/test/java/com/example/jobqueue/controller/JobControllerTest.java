package com.example.jobqueue.controller;

import com.example.jobqueue.config.SecurityConfig;
import com.example.jobqueue.dto.JobStatusResponse;
import com.example.jobqueue.dto.JobSubmitResponse;
import com.example.jobqueue.entity.JobStatus;
import com.example.jobqueue.entity.JobType;
import com.example.jobqueue.exception.AccessDeniedException;
import com.example.jobqueue.exception.JobNotFoundException;
import com.example.jobqueue.security.JwtAuthenticationFilter;
import com.example.jobqueue.security.JwtService;
import com.example.jobqueue.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private JobService jobService;

    private String tokenForUser(UUID userId) {
        return "Bearer " + jwtService.generateToken(userId, "testuser");
    }

    @Test
    void submitJob_shouldReturn201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(jobService.submitJob(eq(userId), any()))
                .thenReturn(new JobSubmitResponse(jobId, JobStatus.PENDING));

        mockMvc.perform(post("/jobs")
                        .header("Authorization", tokenForUser(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WORD_COUNT", "payload": "hello world"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void submitJob_shouldReturn400WhenPayloadBlank() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", tokenForUser(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WORD_COUNT", "payload": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitJob_shouldReturn400WhenTypeMissing() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", tokenForUser(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payload": "hello"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitJob_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WORD_COUNT", "payload": "hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitJob_shouldReturn401WithInvalidToken() throws Exception {
        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WORD_COUNT", "payload": "hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getJobStatus_shouldReturnJob() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        JobStatusResponse response = new JobStatusResponse(
                jobId, JobType.WORD_COUNT, "hello", "1",
                JobStatus.COMPLETED, now, now);

        when(jobService.getJobStatus(userId, jobId)).thenReturn(response);

        mockMvc.perform(get("/jobs/{jobId}", jobId)
                        .header("Authorization", tokenForUser(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.type").value("WORD_COUNT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value("1"));
    }

    @Test
    void getJobStatus_shouldReturn404WhenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(jobService.getJobStatus(userId, jobId)).thenThrow(new JobNotFoundException(jobId));

        mockMvc.perform(get("/jobs/{jobId}", jobId)
                        .header("Authorization", tokenForUser(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJobStatus_shouldReturn403WhenNotOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(jobService.getJobStatus(userId, jobId)).thenThrow(new AccessDeniedException(jobId));

        mockMvc.perform(get("/jobs/{jobId}", jobId)
                        .header("Authorization", tokenForUser(userId)))
                .andExpect(status().isForbidden());
    }
}
