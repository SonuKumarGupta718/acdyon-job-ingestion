package com.sonu.jobingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "job.ingestion.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class JobScheduler {

    private final JobIngestionService jobIngestionService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Periodically triggers the job ingestion process.
     * Overlapping runs are avoided using an AtomicBoolean check.
     */
    @Scheduled(cron = "${job.ingestion.scheduler.cron:0 0/30 * * * ?}")
    public void scheduleIngestion() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                log.info("Scheduled Job Ingestion execution triggered.");
                jobIngestionService.ingestJobs();
                log.info("Scheduled Job Ingestion execution finished successfully.");
            } catch (Exception e) {
                log.error("Scheduled Job Ingestion execution failed: {}", e.getMessage(), e);
            } finally {
                isRunning.set(false);
            }
        } else {
            log.warn("Scheduled Job Ingestion execution skipped: previous ingestion run is still in progress.");
        }
    }
}
