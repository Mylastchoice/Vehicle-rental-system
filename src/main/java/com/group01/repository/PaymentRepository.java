package com.group01.repository;

import com.group01.entity.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBooking_BookingId(Long bookingId);
}
