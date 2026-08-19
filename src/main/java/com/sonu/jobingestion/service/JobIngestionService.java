package com.sonu.jobingestion.service;

import com.sonu.jobingestion.client.ArbeitnowClient;
import com.sonu.jobingestion.client.ArbeitnowResponse;
import com.sonu.jobingestion.client.ArbeitnowResponse.ArbeitnowJobDto;
import com.sonu.jobingestion.model.Job;
import com.sonu.jobingestion.parser.ArbeitnowParser;
import com.sonu.jobingestion.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobIngestionService {

    private final JobRepository jobRepository;
    private final ArbeitnowClient jobSourceClient;
    private final ArbeitnowParser jobParser;

    /**
     * Executes the ingestion pipeline for the first page of jobs.
     *
     * @return the IngestionSummary statistics
     */
    public IngestionSummary ingestJobs() {
        return ingestJobs(1);
    }

    /**
     * Executes the ingestion pipeline for a specific page of jobs.
     *
     * @param page the page number to fetch
     * @return the IngestionSummary statistics
     */
    public IngestionSummary ingestJobs(int page) {
        log.info("Job Ingestion Pipeline Started for page {}", page);
        
        int fetched = 0;
        int parsed = 0;
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        try {
            // 1. FETCH - Request raw job listings from client
            ArbeitnowResponse response = jobSourceClient.fetchJobs(page);
            if (response == null || response.getData() == null) {
                log.warn("Ingestion aborted: No response data returned from external client.");
                return IngestionSummary.builder()
                        .status("SUCCESS")
                        .errorMessage("No job listings returned from the source API.")
                        .build();
            }

            List<ArbeitnowJobDto> rawJobs = response.getData();
            fetched = rawJobs.size();
            log.info("FETCH phase complete. Retrieved {} raw job items from source.", fetched);

            for (ArbeitnowJobDto rawJob : rawJobs) {
                Job parsedJob;
                // 2. PARSE - Convert DTO to internal model with validation inside parser
                try {
                    parsedJob = jobParser.parse(rawJob);
                } catch (Exception e) {
                    log.error("PARSE failed for job slug [{}]: {}", rawJob.getSlug(), e.getMessage());
                    failed++;
                    continue;
                }

                if (parsedJob == null) {
                    // Item was skipped due to validation failure inside parser
                    failed++;
                    continue;
                }

                parsed++;

                // 3. DEDUPLICATE & PERSIST
                try {
                    // Check if job exists in database based on (source + externalId)
                    Optional<Job> existingJobOpt = jobRepository.findBySourceAndExternalId(
                            parsedJob.getSource(), parsedJob.getExternalId());

                    if (existingJobOpt.isEmpty()) {
                        // 3.1. INSERT - Job is new
                        jobRepository.save(parsedJob);
                        inserted++;
                        log.info("INSERTED new job: ID = {}, Source = {}, ExternalId = {}", 
                                parsedJob.getId(), parsedJob.getSource(), parsedJob.getExternalId());
                    } else {
                        // 3.2. UPDATE / DEDUPLICATE - Job already exists, check if changes exist
                        Job existingJob = existingJobOpt.get();
                        if (hasChanges(existingJob, parsedJob)) {
                            updateExistingJob(existingJob, parsedJob);
                            jobRepository.save(existingJob);
                            updated++;
                            log.info("UPDATED modified job: ID = {}, Source = {}, ExternalId = {}", 
                                    existingJob.getId(), existingJob.getSource(), existingJob.getExternalId());
                        } else {
                            skipped++;
                            log.debug("SKIPPED duplicate job (no changes): Source = {}, ExternalId = {}", 
                                    existingJob.getSource(), existingJob.getExternalId());
                        }
                    }
                } catch (Exception e) {
                    log.error("DATABASE PERSISTENCE failed for externalId [{}]: {}", parsedJob.getExternalId(), e.getMessage());
                    failed++;
                }
            }

            log.info("Job Ingestion Pipeline Completed. Summary: Fetched={}, Parsed={}, Inserted={}, Updated={}, Skipped={}, Failed={}",
                    fetched, parsed, inserted, updated, skipped, failed);

            return IngestionSummary.builder()
                    .status("SUCCESS")
                    .fetched(fetched)
                    .parsed(parsed)
                    .inserted(inserted)
                    .updated(updated)
                    .skipped(skipped)
                    .failed(failed)
                    .build();

        } catch (Exception e) {
            log.error("CRITICAL error: Ingestion pipeline execution failed: {}", e.getMessage(), e);
            return IngestionSummary.builder()
                    .status("FAILED")
                    .fetched(fetched)
                    .parsed(parsed)
                    .inserted(inserted)
                    .updated(updated)
                    .skipped(skipped)
                    .failed(fetched - parsed + failed)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Determines whether there are any changes in fields between existing and incoming parsed jobs.
     */
    private boolean hasChanges(Job existing, Job incoming) {
        return !Objects.equals(existing.getTitle(), incoming.getTitle())
                || !Objects.equals(existing.getCompany(), incoming.getCompany())
                || !Objects.equals(existing.getLocation(), incoming.getLocation())
                || !Objects.equals(existing.getDescription(), incoming.getDescription())
                || !Objects.equals(existing.getUrl(), incoming.getUrl())
                || !Objects.equals(existing.getRemote(), incoming.getRemote())
                || !Objects.equals(existing.getTags(), incoming.getTags());
    }

    /**
     * Updates fields of the existing job using new data, updating the scrapedAt timestamp.
     */
    private void updateExistingJob(Job existing, Job incoming) {
        existing.setTitle(incoming.getTitle());
        existing.setCompany(incoming.getCompany());
        existing.setLocation(incoming.getLocation());
        existing.setDescription(incoming.getDescription());
        existing.setUrl(incoming.getUrl());
        existing.setRemote(incoming.getRemote());
        existing.setTags(incoming.getTags());
        existing.setScrapedAt(Instant.now());
    }
}
