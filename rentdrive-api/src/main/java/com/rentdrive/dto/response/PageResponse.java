package com.rentdrive.dto.response;

import org.springframework.data.domain.Page;

import java.util.List; /** Enveloppe paginée générique. */
public record PageResponse<T>(
    List<T> content,
    int     page,
    int     size,
    long    totalElements,
    int     totalPages,
    boolean last
) {
    public static <T> PageResponse<T> of(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                                  p.getTotalElements(), p.getTotalPages(), p.isLast());
    }
}
