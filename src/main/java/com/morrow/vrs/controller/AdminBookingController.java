package com.morrow.vrs.controller;

import com.morrow.vrs.domain.enums.BookingStatus;
import com.morrow.vrs.repository.BookingStatusHistoryRepository;
import com.morrow.vrs.security.CustomUserDetails;
import com.morrow.vrs.service.AdminBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final BookingStatusHistoryRepository historyRepository;

    @GetMapping
    public String listAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.atTime(LocalTime.MAX) : null;

        model.addAttribute("bookings", adminBookingService.listBookings(status, from, to, PageRequest.of(page, 15)));
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
        return "admin/bookings";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(
            @PathVariable Long id,
            @RequestParam BookingStatus targetStatus,
            @RequestParam(required = false, defaultValue = "") String note,
            @AuthenticationPrincipal CustomUserDetails admin,
            RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.updateBookingStatus(id, targetStatus, admin.getId(), note);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành " + targetStatus + " thành công.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @GetMapping("/{id}/history")
    public String viewAuditHistory(@PathVariable Long id, Model model) {
        model.addAttribute("historyList", historyRepository.findByBookingIdOrderByChangedAtDesc(id));
        model.addAttribute("bookingId", id);
        return "admin/booking-history";
    }
}