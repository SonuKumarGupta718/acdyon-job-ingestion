package com.sonu.jobingestion.controller;

import com.sonu.jobingestion.model.Job;
import com.sonu.jobingestion.service.IngestionSummary;
import com.sonu.jobingestion.service.JobIngestionService;
import com.sonu.jobingestion.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class JobController {

    private final JobService jobService;
    private final JobIngestionService jobIngestionService;

    /**
     * Health check endpoint to monitor application status.
     *
     * @return 200 OK status message
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getHealth() {
        log.info("GET /api/health invoked.");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Job Ingestion Service is active and running."
        ));
    }

    /**
     * Triggers the job ingestion pipeline manually.
     *
     * @param page optional page number from the source to ingest (default: 1)
     * @return the ingestion summary statistics
     */
    @PostMapping("/jobs/ingest")
    public ResponseEntity<IngestionSummary> ingestJobs(
            @RequestParam(name = "page", defaultValue = "1") int page) {
        log.info("POST /api/jobs/ingest invoked. Manual ingestion triggered for source page: {}", page);
        IngestionSummary summary = jobIngestionService.ingestJobs(page);
        
        if ("FAILED".equals(summary.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(summary);
        }
        return ResponseEntity.ok(summary);
    }

    /**
     * Searches and lists jobs dynamically with sorting, filtering, and pagination support.
     *
     * @param keyword  optional keyword filter (company, title, description, tags)
     * @param location optional location filter
     * @param page     page number (0-indexed, default: 0)
     * @param size     items per page (default: 20)
     * @return paginated list of job documents
     */
    @GetMapping("/jobs")
    public ResponseEntity<PagedResponse<Job>> getJobs(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        log.info("GET /api/jobs invoked. Parameters: keyword = '{}', location = '{}', page = {}, size = {}", 
                keyword, location, page, size);
        
        // Return results sorted by the publication date in descending order (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<Job> result = jobService.searchJobs(keyword, location, pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(result));
    }

    /**
     * Fetches details of a single job by its MongoDB document ID.
     *
     * @param id the MongoDB ID of the job
     * @return the Job document details or 404 NOT FOUND
     */
    @GetMapping("/jobs/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable("id") String id) {
        log.info("GET /api/jobs/{} invoked.", id);
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Job with ID {} not found.", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }
}
