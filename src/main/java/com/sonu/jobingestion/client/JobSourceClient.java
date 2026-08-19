package com.sonu.jobingestion.client;

/**
 * Generic interface for fetching raw job response wrappers from any external source.
 *
 * @param <T> the response wrapper DTO type
 */
public interface JobSourceClient<T> {
    
    /**
     * Fetches raw job response from the external source.
     *
     * @param page the page number to fetch
     * @return the raw response DTO
     */
    T fetchJobs(int page);
}
