package com.rafa.jdmatch.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A stable JSON shape for a page of results. Spring Data's {@code Page} serializes with
 * an unstable structure, so endpoints return this instead.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
