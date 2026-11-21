package com.group01.repository;

import com.group01.entity.Booking;
import com.group01.enums.BookingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByStatus(BookingStatus status);
}
