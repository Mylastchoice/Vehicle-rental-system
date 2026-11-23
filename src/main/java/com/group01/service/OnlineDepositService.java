package com.group01.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.group01.dto.BookingViewResponse;
import com.group01.repository.BookingManagementRepository;
import com.group01.repository.BookingManagementRepository.Deposit;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OnlineDepositService {
    private final BookingManagementRepository repository;
    private final StripeCheckoutGateway gateway;

    public OnlineDepositService(BookingManagementRepository repository, StripeCheckoutGateway gateway) {
        this.repository = repository;
        this.gateway = gateway;
    }

    public boolean enabled() {
        return gateway.enabled();
    }

    public String status(Long bookingId) {
        Deposit deposit = repository.findDeposit(bookingId);
        return deposit == null ? null : deposit.status();
    }

    @Transactional
    public URI open(Long bookingId, Long memberId) {
        BookingViewResponse booking = ownedBooking(bookingId, memberId);
        if (!"ONLINE".equals(booking.paymentMethod()) || !"Pending".equals(booking.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking cannot accept an online deposit");
        }
        Deposit deposit = requireDeposit(bookingId);
        if ("Paid".equals(deposit.status())) {
            return URI.create("/bookings/" + bookingId);
        }
        String previousSession = deposit.sessionId();
        if (previousSession != null) {
            JsonNode session = gateway.retrieve(previousSession);
            if (recordIfPaid(deposit, session)) {
                return URI.create("/bookings/" + bookingId);
            }
            String status = session.path("status").asText();
            if ("open".equals(status)) {
                return checkoutUrl(session);
            }
            if (!"expired".equals(status)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This deposit is being processed");
            }
        }
        String key = deposit.checkoutKey() + ":" + (previousSession == null ? "first" : previousSession);
        JsonNode session = gateway.create(bookingId, deposit.amount().longValueExact(), key);
        URI link = checkoutUrl(session);
        String sessionId = session.path("id").asText();
        if (!sessionId.startsWith("cs_test_")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Stripe sandbox session");
        }
        repository.saveCheckoutSession(deposit.id(), sessionId);
        return link;
    }

    @Transactional
    public boolean reconcile(Long bookingId, Long memberId) {
        BookingViewResponse booking = ownedBooking(bookingId, memberId);
        if (!"ONLINE".equals(booking.paymentMethod())) {
            return false;
        }
        Deposit deposit = requireDeposit(bookingId);
        return "Paid".equals(deposit.status()) || (deposit.sessionId() != null
                && recordIfPaid(deposit, gateway.retrieve(deposit.sessionId())));
    }

    @Transactional
    public void reconcileEvent(String sessionId) {
        Long bookingId = repository.findBookingForSession(sessionId);
        if (bookingId == null || repository.lockById(bookingId) == null) {
            return;
        }
        Deposit deposit = requireDeposit(bookingId);
        if (sessionId.equals(deposit.sessionId()) && !"Paid".equals(deposit.status())) {
            recordIfPaid(deposit, gateway.retrieve(sessionId));
        }
    }

    // The caller holds the booking lock, so cancellation and checkout cannot open competing sessions.
    public boolean closeForCancellation(Long bookingId) {
        Deposit deposit = requireDeposit(bookingId);
        if ("Paid".equals(deposit.status())) {
            return true;
        }
        if (deposit.sessionId() == null) {
            return false;
        }
        JsonNode session = gateway.retrieve(deposit.sessionId());
        if (recordIfPaid(deposit, session)) {
            return true;
        }
        if ("open".equals(session.path("status").asText())) {
            gateway.expire(deposit.sessionId());
        } else if (!"expired".equals(session.path("status").asText())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for the deposit to finish processing");
        }
        return false;
    }

    private BookingViewResponse ownedBooking(Long id, Long memberId) {
        BookingViewResponse booking = repository.lockById(id);
        if (booking == null || !memberId.equals(booking.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        return booking;
    }

    private Deposit requireDeposit(Long id) {
        Deposit deposit = repository.findDeposit(id);
        if (deposit == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit record not found");
        }
        return deposit;
    }

    private boolean recordIfPaid(Deposit deposit, JsonNode session) {
        if (!"paid".equals(session.path("payment_status").asText())) {
            return false;
        }
        if (!deposit.sessionId().equals(session.path("id").asText())
                || !"vnd".equals(session.path("currency").asText())
                || !session.path("amount_total").isIntegralNumber()
                || session.path("amount_total").longValue() != deposit.amount().longValueExact()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe deposit verification failed");
        }
        repository.markDepositPaid(deposit.id());
        return true;
    }

    private URI checkoutUrl(JsonNode session) {
        URI url = URI.create(session.path("url").asText());
        if (!"https".equals(url.getScheme()) || !"checkout.stripe.com".equals(url.getHost())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Stripe sandbox checkout URL");
        }
        return url;
    }
}
