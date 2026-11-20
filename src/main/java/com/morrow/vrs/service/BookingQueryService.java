package com.morrow.vrs.service;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingQueryService {

    private final BookingRepository bookingRepository;

    public Page<Booking> getMemberBookings(Long currentUserId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable);
    }

    public Booking getMemberBookingDetail(Long bookingId, Long currentUserId) {
        return bookingRepository.findByIdAndUserId(bookingId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy đơn thuê hoặc bạn không có quyền truy cập."));
    }
}