package com.morrow.vrs.controller;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.security.CustomUserDetails;
import com.morrow.vrs.service.BookingCancellationService;
import com.morrow.vrs.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingQueryService bookingQueryService;
    private final BookingCancellationService cancellationService;

    @GetMapping
    public String viewMyBookings(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<Booking> bookings = bookingQueryService.getMemberBookings(user.getId(), PageRequest.of(page, size));
        model.addAttribute("bookings", bookings);
        return "booking/list";
    }

    @GetMapping("/{id}")
    public String viewBookingDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user,
            Model model) {
        Booking booking = bookingQueryService.getMemberBookingDetail(id, user.getId());
        model.addAttribute("booking", booking);
        return "booking/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Khách hàng yêu cầu hủy") String reason,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirectAttributes) {
        try {
            var result = cancellationService.cancelByMember(id, user.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hủy đơn thành công. " + result.refundPolicyNote());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/bookings/" + id;
    }
}