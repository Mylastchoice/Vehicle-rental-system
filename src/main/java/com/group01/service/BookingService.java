package com.group01.service;

import com.group01.dto.BookingDetailResponse;
import com.group01.dto.BookingPageResponse;
import com.group01.dto.BookingViewResponse;
import com.group01.dto.BookingVehicleResponse;
import com.group01.enums.BookingStatus;
import com.group01.repository.BookingManagementRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** M4: booking history, member cancellation and administrator state changes. */
@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final BookingManagementRepository repository;
    private final BookingNotificationService notifications;
    private final OnlineDepositService onlineDeposits;

    public BookingService(BookingManagementRepository repository,
                          BookingNotificationService notifications, OnlineDepositService onlineDeposits) {
        this.repository = repository;
        this.notifications = notifications;
        this.onlineDeposits = onlineDeposits;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be >= 0 and size must be 1..100");
        }
    }

    @Transactional(readOnly = true)
    public BookingPageResponse memberHistory(Long memberId, int page, int size) {
        validatePage(page, size);
        return repository.listForMember(memberId, page, size);
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse memberDetails(Long memberId, Long id) {
        BookingDetailResponse detail = repository.findDetails(id);
        if (detail == null || !memberId.equals(detail.booking().userId())) {
            // Hide both non-existent bookings and bookings owned by someone else.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        return detail;
    }

    @Transactional
    public Map<String, Object> memberCancel(Long memberId, Long id) {
        BookingViewResponse current = requireLockedBooking(id);
        if (!memberId.equals(current.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        BookingStatus from = statusOf(current.status());
        if (from != BookingStatus.PENDING && from != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking cannot be cancelled");
        }
        // Database stores dates, not pickup times; assume pickup starts at 00:00 local time.
        long hoursToPickup = Duration.between(LocalDateTime.now(),
                current.pickupDate().atStartOfDay()).toHours();
        if (hoursToPickup < 0 || !LocalDateTime.now().isBefore(current.pickupDate().atStartOfDay())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pickup has already started");
        }
        boolean hasDeposit = !"ONLINE".equals(current.paymentMethod()) || onlineDeposits.closeForCancellation(id);
        BigDecimal refund = hoursToPickup >= 24 && hasDeposit ? current.depositAmount() : BigDecimal.ZERO;
        repository.updateStatus(id, BookingStatus.CANCELLED);
        repository.recordPendingRefund(id, refund);
        audit(id, memberId.toString(), from, BookingStatus.CANCELLED);
        notifications.notifyStatusChange(repository.findMemberEmail(memberId), id, "Cancelled");
        return Map.of("bookingId", id, "status", "Cancelled", "refundRequested", refund,
                "refundStatus", refund.signum() > 0 ? "Pending" : "Not eligible",
                "note", refund.signum() == 0 ? "DEC-03 PENDING: manual review if needed" : "");
    }

    @Transactional(readOnly = true)
    public BookingPageResponse adminHistory(BookingStatus status, LocalDate from,
                                             LocalDate to, int page, int size) {
        validatePage(page, size);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }
        return repository.listForAdmin(status, from, to, page, size);
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse adminDetails(Long id) {
        BookingDetailResponse result = repository.findDetails(id);
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        return result;
    }

    @Transactional
    public void requestEarlyReturn(Long memberId, Long id, LocalDate date) {
        BookingViewResponse booking = requireLockedBooking(id);
        if (!memberId.equals(booking.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        if (statusOf(booking.status()) != BookingStatus.PICKED_UP || booking.requestedReturnDate() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking cannot request an early return");
        }
        if (date == null || date.isBefore(LocalDate.now()) || date.isBefore(booking.pickupDate())
                || !date.isBefore(booking.returnDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a return date from today, on or after pickup, and before the scheduled return");
        }
        repository.saveReturnRequest(id, date);
    }

    @Transactional
    public Map<String, Object> adminUpdateStatus(Long id, BookingStatus target, String adminEmail) {
        BookingViewResponse current = requireLockedBooking(id);
        BookingStatus from = statusOf(current.status());
        boolean allowed = (from == BookingStatus.PENDING &&
                (target == BookingStatus.CONFIRMED || target == BookingStatus.CANCELLED))
                || (from == BookingStatus.CONFIRMED &&
                (target == BookingStatus.PICKED_UP || target == BookingStatus.CANCELLED))
                || (from == BookingStatus.PICKED_UP && target == BookingStatus.COMPLETED);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid booking status transition: " + from + " -> " + target);
        }
        if (target == BookingStatus.CONFIRMED) {
            if ("ONLINE".equals(current.paymentMethod()) && !"Paid".equals(onlineDeposits.status(id))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The online deposit must be paid before confirmation");
            }
            BookingDetailResponse detail = repository.findDetails(id);
            if (detail == null || detail.vehicles().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking has no vehicles");
            }
            // Same sorted vehicle locking convention as M3 checkout.
            List<BookingVehicleResponse> items = detail.vehicles();
            for (BookingVehicleResponse vehicle : items) repository.lockVehicle(vehicle.vehicleCode());
            for (BookingVehicleResponse vehicle : items) {
                if (repository.hasConfirmationConflict(id, vehicle.vehicleCode(),
                        vehicle.pickupDate(), vehicle.returnDate())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Vehicle schedule conflict: " + vehicle.vehicleCode());
                }
            }
        }
        boolean refundDue = target == BookingStatus.CANCELLED
                && (!"ONLINE".equals(current.paymentMethod()) || onlineDeposits.closeForCancellation(id));
        if (target == BookingStatus.COMPLETED) {
            LocalDate actualDate = LocalDate.now();
            if (actualDate.isBefore(current.pickupDate())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot record a return before pickup");
            }
            for (String vehicleCode : repository.findVehicleCodes(id)) {
                repository.countCompletedRental(vehicleCode);
            }
            repository.completeReturn(id, actualDate);
        } else {
            repository.updateStatus(id, target);
        }
        BigDecimal refund = refundDue ? current.depositAmount() : BigDecimal.ZERO;
        repository.recordPendingRefund(id, refund);
        audit(id, adminEmail, from, target);
        notifications.notifyStatusChange(repository.findMemberEmail(current.userId()),
                id, target.getDbValue());
        return Map.of("bookingId", id, "previousStatus", from.getDbValue(),
                "status", target.getDbValue(), "refundRequested", refund,
                "refundStatus", refund.signum() > 0 ? "Pending" : "Not applicable");
    }

    private BookingViewResponse requireLockedBooking(Long id) {
        BookingViewResponse value = repository.lockById(id);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        return value;
    }

    private BookingStatus statusOf(String dbStatus) {
        try {
            return BookingStatus.fromDbValue(dbStatus);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unrecognized booking status");
        }
    }

    /** Audited in server logs only; database has no booking audit table. */
    private void audit(Long id, String actor, BookingStatus before, BookingStatus after) {
        log.info("M4 booking status change booking={} actor={} from={} to={}",
                id, actor, before.getDbValue(), after.getDbValue());
    }
}
