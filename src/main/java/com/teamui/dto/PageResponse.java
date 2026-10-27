package com.teamui.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pagination block placed in {@code data} of the envelope:
 * {@code { "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }}.
 *
 * @param content       items on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements total number of items across all pages
 * @param totalPages    total number of pages
 * @param <T>           item type
 * @author TeamUI
 * @since 0.0.1
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Converts a Spring Data page.
     *
     * @param page Spring Data page
     * @param <T>  item type
     * @return pagination block in the constitution's shape
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
