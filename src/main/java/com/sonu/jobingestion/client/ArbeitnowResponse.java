package com.sonu.jobingestion.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArbeitnowResponse {

    @JsonProperty("data")
    private List<ArbeitnowJobDto> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArbeitnowJobDto {
        @JsonProperty("slug")
        private String slug;

        @JsonProperty("company_name")
        private String companyName;

        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("remote")
        private Boolean remote;

        @JsonProperty("url")
        private String url;

        @JsonProperty("tags")
        private List<String> tags;

        @JsonProperty("job_types")
        private List<String> jobTypes;

        @JsonProperty("location")
        private String location;

        @JsonProperty("created_at")
        private Long createdAt;
    }
}
