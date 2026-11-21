package com.morrow.vrs.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BookingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    private BigDecimal dailyPrice;
    private Integer days;
    private BigDecimal subtotal;
    private LocalDateTime pickupDate;
    private LocalDateTime returnDate;
}
