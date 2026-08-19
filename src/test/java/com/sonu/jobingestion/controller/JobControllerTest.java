package com.sonu.jobingestion.controller;

import com.sonu.jobingestion.model.Job;
import com.sonu.jobingestion.service.IngestionSummary;
import com.sonu.jobingestion.service.JobIngestionService;
import com.sonu.jobingestion.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JobService jobService;

    @Mock
    private JobIngestionService jobIngestionService;

    @BeforeEach
    void setUp() {
        JobController controller = new JobController(jobService, jobIngestionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testGetHealth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void testIngestJobs_Success() throws Exception {
        IngestionSummary summary = IngestionSummary.builder()
                .status("SUCCESS")
                .fetched(10)
                .parsed(10)
                .inserted(8)
                .updated(2)
                .skipped(0)
                .failed(0)
                .build();

        when(jobIngestionService.ingestJobs(1)).thenReturn(summary);

        mockMvc.perform(post("/api/jobs/ingest")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fetched").value(10))
                .andExpect(jsonPath("$.inserted").value(8))
                .andExpect(jsonPath("$.updated").value(2));
    }

    @Test
    void testGetJobs_WithFilters() throws Exception {
        Job job = Job.builder().id("job1").title("Data Scientist").company("Acme Corp").build();
        when(jobService.searchJobs(eq("Data"), eq("India"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(job)));

        mockMvc.perform(get("/api/jobs")
                        .param("keyword", "Data")
                        .param("location", "India")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Data Scientist"))
                .andExpect(jsonPath("$.content[0].company").value("Acme Corp"));
    }

    @Test
    void testGetJobById_Found() throws Exception {
        Job job = Job.builder().id("job1").title("Data Scientist").company("Acme Corp").build();
        when(jobService.getJobById("job1")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/jobs/job1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Data Scientist"))
                .andExpect(jsonPath("$.company").value("Acme Corp"));
    }

    @Test
    void testGetJobById_NotFound() throws Exception {
        when(jobService.getJobById("job99")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/job99"))
                .andExpect(status().isNotFound());
    }
}
