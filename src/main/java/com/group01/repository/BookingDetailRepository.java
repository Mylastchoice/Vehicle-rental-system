package com.group01.repository;

import com.group01.entity.BookingDetail;
import com.group01.enums.BookingStatus;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    /**
     * Number of bookings of one vehicle that overlap the period [pickup, return) and are in one of the given statuses.
     * Two periods overlap when start1 &lt; end2 and start2 &lt; end1 (back-to-back rentals are allowed).
     * M3 owns the availability business rules; this query is the shared database part.
     */
    @Query("""
            select count(d) from BookingDetail d
            where d.vehicle.vehicleCode = :vehicleCode
              and d.booking.status in :statuses
              and d.pickupDate < :returnDate
              and :pickupDate < d.returnDate
            """)
    long countOverlapping(@Param("vehicleCode") String vehicleCode,
                          @Param("pickupDate") LocalDate pickupDate,
                          @Param("returnDate") LocalDate returnDate,
                          @Param("statuses") Collection<BookingStatus> statuses);
}
