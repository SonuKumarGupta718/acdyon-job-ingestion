package com.sonu.jobingestion.parser;

import com.sonu.jobingestion.client.ArbeitnowResponse.ArbeitnowJobDto;
import com.sonu.jobingestion.model.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArbeitnowParserTest {

    private ArbeitnowParser parser;

    @BeforeEach
    void setUp() {
        parser = new ArbeitnowParser();
    }

    @Test
    void testParse_Success() {
        ArbeitnowJobDto dto = ArbeitnowJobDto.builder()
                .slug("software-engineer-berlin-123")
                .companyName("Test Tech Co")
                .title("Software Engineer")
                .description("<p>Awesome role</p>")
                .remote(true)
                .url("https://example.com/jobs/1")
                .location("Berlin")
                .createdAt(1700000000L)
                .tags(List.of("Java", "Spring Boot"))
                .build();

        Job job = parser.parse(dto);

        assertThat(job).isNotNull();
        assertThat(job.getExternalId()).isEqualTo("software-engineer-berlin-123");
        assertThat(job.getCompany()).isEqualTo("Test Tech Co");
        assertThat(job.getTitle()).isEqualTo("Software Engineer");
        assertThat(job.getDescription()).isEqualTo("<p>Awesome role</p>");
        assertThat(job.getRemote()).isTrue();
        assertThat(job.getUrl()).isEqualTo("https://example.com/jobs/1");
        assertThat(job.getLocation()).isEqualTo("Berlin");
        assertThat(job.getSource()).isEqualTo("ARBEITNOW");
        assertThat(job.getPublishedAt()).isEqualTo(Instant.ofEpochSecond(1700000000L));
        assertThat(job.getTags()).containsExactly("Java", "Spring Boot");
        assertThat(job.getScrapedAt()).isNotNull();
    }

    @Test
    void testParse_MissingCrucialFields_ReturnsNull() {
        // Missing slug
        ArbeitnowJobDto missingSlugDto = ArbeitnowJobDto.builder()
                .companyName("Test")
                .title("Software Engineer")
                .build();

        // Missing title
        ArbeitnowJobDto missingTitleDto = ArbeitnowJobDto.builder()
                .slug("slug-123")
                .companyName("Test")
                .build();

        // Missing company name
        ArbeitnowJobDto missingCompanyDto = ArbeitnowJobDto.builder()
                .slug("slug-123")
                .title("Developer")
                .build();

        assertThat(parser.parse(missingSlugDto)).isNull();
        assertThat(parser.parse(missingTitleDto)).isNull();
        assertThat(parser.parse(missingCompanyDto)).isNull();
    }

    @Test
    void testParse_NullCreatedAt_DefaultsToNow() {
        ArbeitnowJobDto dto = ArbeitnowJobDto.builder()
                .slug("slug-123")
                .companyName("Test Company")
                .title("Developer")
                .createdAt(null)
                .build();

        Job job = parser.parse(dto);

        assertThat(job).isNotNull();
        assertThat(job.getPublishedAt()).isBeforeOrEqualTo(Instant.now());
    }
}
