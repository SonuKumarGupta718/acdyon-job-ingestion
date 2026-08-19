package com.sonu.jobingestion.service;

import com.sonu.jobingestion.model.Job;
import com.sonu.jobingestion.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Finds a single job listing by ID.
     *
     * @param id the job document ID
     * @return an Optional of Job
     */
    public Optional<Job> getJobById(String id) {
        return jobRepository.findById(id);
    }

    /**
     * Dynamic keyword search and location filter with pagination.
     *
     * @param keyword  optional search term (matches title, company, description, or tags)
     * @param location optional location filter (case-insensitive)
     * @param pageable page settings (page number, size)
     * @return page of Job listings
     */
    public Page<Job> searchJobs(String keyword, String location, Pageable pageable) {
        Query query = new Query().with(pageable);
        List<Criteria> criteriaList = new ArrayList<>();

        // Add keyword search criteria (checks title, company, description, and tags)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchPattern = keyword.trim();
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(searchPattern, "i"),
                    Criteria.where("company").regex(searchPattern, "i"),
                    Criteria.where("description").regex(searchPattern, "i"),
                    Criteria.where("tags").regex(searchPattern, "i")
            );
            criteriaList.add(keywordCriteria);
        }

        // Add location criteria
        if (location != null && !location.trim().isEmpty()) {
            criteriaList.add(Criteria.where("location").regex(location.trim(), "i"));
        }

        // Combine criteria using AND
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Execute queries and get page statistics
        List<Job> jobs = mongoTemplate.find(query, Job.class);
        return PageableExecutionUtils.getPage(
                jobs,
                pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Job.class)
        );
    }
}
