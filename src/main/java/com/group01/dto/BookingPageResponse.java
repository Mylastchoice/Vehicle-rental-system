package com.group01.dto;

import java.util.List;

/** Page numbers are zero-based to match the other /api endpoints. */
public record BookingPageResponse(List<BookingViewResponse> items,
                                  long total, int page, int size, int totalPages) {
    public boolean hasPrevious() { return page > 0; }
    public boolean hasNext() { return page + 1 < totalPages; }
}
