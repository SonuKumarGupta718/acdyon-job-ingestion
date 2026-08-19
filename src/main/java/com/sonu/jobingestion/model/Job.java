package com.sonu.jobingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "jobs")
@CompoundIndex(name = "source_externalId_idx", def = "{'source': 1, 'externalId': 1}", unique = true)
public class Job {

    @Id
    private String id;
    
    private String title;
    private String company;
    private String location;
    private String description;
    private String url;
    private String source;
    private Instant publishedAt;
    private Instant scrapedAt;
    private String externalId;
    private Boolean remote;
    private List<String> tags;
}
