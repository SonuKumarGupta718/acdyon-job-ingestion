package com.sonu.jobingestion.parser;

import com.sonu.jobingestion.model.Job;

/**
 * Interface for parsing external source DTOs into the internal Job domain model.
 *
 * @param <S> the source DTO type
 */
public interface JobParser<S> {
    
    /**
     * Parses the external source DTO into a Job entity.
     *
     * @param source the source DTO
     * @return the mapped Job entity, or null if mapping fails
     */
    Job parse(S source);
}
