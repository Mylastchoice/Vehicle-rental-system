package com.morrow.vrs.repository;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // UC09: Danh sách booking của member, mới nhất trước
    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // UC09 & UC10: Lấy chi tiết đúng chủ đơn (BR08)
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    // UC13: Lọc danh sách cho Admin
    @Query("""
        SELECT b FROM Booking b
        WHERE (:status IS NULL OR b.status = :status)
          AND (:fromDate IS NULL OR b.pickupDate >= :fromDate)
          AND (:toDate IS NULL OR b.pickupDate <= :toDate)
        ORDER BY b.createdAt DESC
    """)
    Page<Booking> searchForAdmin(
            @Param("status") BookingStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}