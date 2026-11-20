package com.morrow.vrs.domain;

import com.morrow.vrs.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false)
    private BookingStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private BookingStatus newStatus;

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @Column(name = "note")
    private String note;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}