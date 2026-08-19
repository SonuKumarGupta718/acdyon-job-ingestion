package com.sonu.jobingestion.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionSummary {
    private String status;
    private int fetched;
    private int parsed;
    private int inserted;
    private int updated;
    private int skipped;
    private int failed;
    private String errorMessage;
}
