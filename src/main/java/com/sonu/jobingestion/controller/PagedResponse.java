package com.sonu.jobingestion.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    /**
     * Converts a Spring Data Page object to a PagedResponse DTO.
     *
     * @param page the Page object
     * @param <T> the content type
     * @return the mapped PagedResponse DTO
     */
    public static <T> PagedResponse<T> fromPage(Page<T> page) {
        if (page == null) {
            return null;
        }
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
