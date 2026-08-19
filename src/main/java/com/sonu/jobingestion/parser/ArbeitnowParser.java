package com.sonu.jobingestion.parser;

import com.sonu.jobingestion.client.ArbeitnowResponse.ArbeitnowJobDto;
import com.sonu.jobingestion.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

@Component
@Slf4j
public class ArbeitnowParser implements JobParser<ArbeitnowJobDto> {

    @Override
    public Job parse(ArbeitnowJobDto dto) {
        if (dto == null) {
            return null;
        }

        // Validate crucial fields that shouldn't be empty
        if (dto.getSlug() == null || dto.getSlug().trim().isEmpty()) {
            log.warn("Skipping job record: external ID (slug) is missing.");
            return null;
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            log.warn("Skipping job record with slug {}: title is missing.", dto.getSlug());
            return null;
        }
        if (dto.getCompanyName() == null || dto.getCompanyName().trim().isEmpty()) {
            log.warn("Skipping job record with slug {}: company name is missing.", dto.getSlug());
            return null;
        }

        // Handle published date safely (default to now if missing/malformed)
        Instant publishedAt;
        if (dto.getCreatedAt() != null) {
            try {
                publishedAt = Instant.ofEpochSecond(dto.getCreatedAt());
            } catch (Exception e) {
                log.warn("Failed to parse created_at timestamp '{}' for slug {}. Defaulting to current time.", dto.getCreatedAt(), dto.getSlug());
                publishedAt = Instant.now();
            }
        } else {
            publishedAt = Instant.now();
        }

        return Job.builder()
                .externalId(dto.getSlug().trim())
                .title(dto.getTitle().trim())
                .company(dto.getCompanyName().trim())
                .location(dto.getLocation() != null ? dto.getLocation().trim() : "Remote / Unspecified")
                .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                .url(dto.getUrl() != null ? dto.getUrl().trim() : "")
                .source("ARBEITNOW")
                .publishedAt(publishedAt)
                .scrapedAt(Instant.now())
                .remote(dto.getRemote() != null ? dto.getRemote() : false)
                .tags(dto.getTags() != null ? new ArrayList<>(dto.getTags()) : new ArrayList<>())
                .build();
    }
}
