package com.group01.controller;

import com.group01.dto.BookingDetailResponse;
import com.group01.dto.BookingPageResponse;
import com.group01.enums.BookingStatus;
import com.group01.service.BookingService;
import com.group01.service.OnlineDepositService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** M4 administration endpoints, additionally protected by SecurityConfig. */
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class BookingAdminController {
    private final BookingService service;
    private final OnlineDepositService payments;

    public BookingAdminController(BookingService service, OnlineDepositService payments) {
        this.service = service;
        this.payments = payments;
    }

    private BookingStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return BookingStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid booking status");
        }
    }

    private BookingStatus requiredStatus(String value) {
        BookingStatus status = parseStatus(value);
        if (status == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        return status;
    }

    @GetMapping("/admin/bookings")
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        BookingPageResponse history = service.adminHistory(parseStatus(status), from, to, page, 15);
        model.addAttribute("history", history);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("statusFilter", status == null ? "" : status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "booking/admin-list";
    }

    @GetMapping("/admin/bookings/{id}")
    public String detail(@PathVariable Long id, Model model) {
        BookingDetailResponse detail = service.adminDetails(id);
        model.addAttribute("detail", detail);
        if ("ONLINE".equals(detail.booking().paymentMethod())) {
            model.addAttribute("depositStatus", payments.status(id));
        }
        return "booking/admin-detail";
    }

    @PostMapping("/admin/bookings/{id}/status")
    public String setStatus(@PathVariable Long id, @RequestParam String status,
                            Principal principal, RedirectAttributes flash) {
        service.adminUpdateStatus(id, requiredStatus(status), principal.getName());
        flash.addFlashAttribute("message", "Booking status updated");
        return "redirect:/admin/bookings/" + id;
    }

    @GetMapping("/api/admin/bookings")
    @ResponseBody
    public BookingPageResponse apiList(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "15") int size) {
        return service.adminHistory(parseStatus(status), from, to, page, size);
    }

    @GetMapping("/api/admin/bookings/{id}")
    @ResponseBody
    public BookingDetailResponse apiDetail(@PathVariable Long id) {
        return service.adminDetails(id);
    }

    public record StatusRequest(String status) { }

    @PatchMapping("/api/admin/bookings/{id}/status")
    @ResponseBody
    public Map<String, Object> apiStatus(@PathVariable Long id, @RequestBody StatusRequest request,
                                          Principal principal) {
        return service.adminUpdateStatus(id, requiredStatus(request.status()), principal.getName());
    }
}
