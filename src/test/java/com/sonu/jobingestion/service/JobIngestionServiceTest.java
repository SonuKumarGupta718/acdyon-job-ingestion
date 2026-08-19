package com.sonu.jobingestion.service;

import com.sonu.jobingestion.client.ArbeitnowClient;
import com.sonu.jobingestion.client.ArbeitnowResponse;
import com.sonu.jobingestion.client.ArbeitnowResponse.ArbeitnowJobDto;
import com.sonu.jobingestion.model.Job;
import com.sonu.jobingestion.parser.ArbeitnowParser;
import com.sonu.jobingestion.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobIngestionServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ArbeitnowClient jobSourceClient;

    @Mock
    private ArbeitnowParser jobParser;

    @InjectMocks
    private JobIngestionService jobIngestionService;

    private ArbeitnowResponse mockResponse;
    private ArbeitnowJobDto mockDto;
    private Job mockJob;

    @BeforeEach
    void setUp() {
        mockDto = ArbeitnowJobDto.builder()
                .slug("test-slug-1")
                .companyName("Test Corp")
                .title("Software Engineer")
                .build();

        mockResponse = ArbeitnowResponse.builder()
                .data(List.of(mockDto))
                .build();

        mockJob = Job.builder()
                .source("ARBEITNOW")
                .externalId("test-slug-1")
                .title("Software Engineer")
                .company("Test Corp")
                .location("Berlin")
                .description("Awesome job description")
                .url("https://example.com/job/1")
                .remote(true)
                .tags(List.of("Java", "Spring"))
                .publishedAt(Instant.now())
                .scrapedAt(Instant.now())
                .build();
    }

    @Test
    void testIngestJobs_InsertNewJob() {
        when(jobSourceClient.fetchJobs(1)).thenReturn(mockResponse);
        when(jobParser.parse(mockDto)).thenReturn(mockJob);
        when(jobRepository.findBySourceAndExternalId("ARBEITNOW", "test-slug-1")).thenReturn(Optional.empty());

        IngestionSummary summary = jobIngestionService.ingestJobs(1);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getFetched()).isEqualTo(1);
        assertThat(summary.getParsed()).isEqualTo(1);
        assertThat(summary.getInserted()).isEqualTo(1);
        assertThat(summary.getUpdated()).isEqualTo(0);
        assertThat(summary.getSkipped()).isEqualTo(0);
        assertThat(summary.getFailed()).isEqualTo(0);

        verify(jobRepository, times(1)).save(mockJob);
    }

    @Test
    void testIngestJobs_UpdateExistingJob_WithChanges() {
        Job existingJob = Job.builder()
                .id("some-mongo-id")
                .source("ARBEITNOW")
                .externalId("test-slug-1")
                .title("Old Title")
                .company("Test Corp")
                .location("Berlin")
                .description("Awesome job description")
                .url("https://example.com/job/1")
                .remote(true)
                .tags(List.of("Java", "Spring"))
                .publishedAt(Instant.now())
                .scrapedAt(Instant.now().minusSeconds(3600))
                .build();

        when(jobSourceClient.fetchJobs(1)).thenReturn(mockResponse);
        when(jobParser.parse(mockDto)).thenReturn(mockJob);
        when(jobRepository.findBySourceAndExternalId("ARBEITNOW", "test-slug-1")).thenReturn(Optional.of(existingJob));

        IngestionSummary summary = jobIngestionService.ingestJobs(1);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getFetched()).isEqualTo(1);
        assertThat(summary.getParsed()).isEqualTo(1);
        assertThat(summary.getInserted()).isEqualTo(0);
        assertThat(summary.getUpdated()).isEqualTo(1);
        assertThat(summary.getSkipped()).isEqualTo(0);
        assertThat(summary.getFailed()).isEqualTo(0);

        verify(jobRepository, times(1)).save(existingJob);
        assertThat(existingJob.getTitle()).isEqualTo("Software Engineer"); // Updated to match incoming
    }

    @Test
    void testIngestJobs_SkipExistingJob_NoChanges() {
        Job existingJob = Job.builder()
                .id("some-mongo-id")
                .source("ARBEITNOW")
                .externalId("test-slug-1")
                .title("Software Engineer")
                .company("Test Corp")
                .location("Berlin")
                .description("Awesome job description")
                .url("https://example.com/job/1")
                .remote(true)
                .tags(List.of("Java", "Spring"))
                .publishedAt(Instant.now())
                .scrapedAt(Instant.now().minusSeconds(3600))
                .build();

        when(jobSourceClient.fetchJobs(1)).thenReturn(mockResponse);
        when(jobParser.parse(mockDto)).thenReturn(mockJob);
        when(jobRepository.findBySourceAndExternalId("ARBEITNOW", "test-slug-1")).thenReturn(Optional.of(existingJob));

        IngestionSummary summary = jobIngestionService.ingestJobs(1);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getFetched()).isEqualTo(1);
        assertThat(summary.getParsed()).isEqualTo(1);
        assertThat(summary.getInserted()).isEqualTo(0);
        assertThat(summary.getUpdated()).isEqualTo(0);
        assertThat(summary.getSkipped()).isEqualTo(1);
        assertThat(summary.getFailed()).isEqualTo(0);

        verify(jobRepository, never()).save(any());
    }
}
