package com.group01.controller;

import com.group01.dto.BookingDetailResponse;
import com.group01.dto.BookingPageResponse;
import com.group01.service.BookingService;
import com.group01.service.CurrentMemberService;
import com.group01.service.OnlineDepositService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

/** M4 member booking pages + JSON endpoints (UC view/cancel). */
@Controller
public class BookingController {
    private final BookingService service;
    private final CurrentMemberService member;
    private final OnlineDepositService payments;

    public BookingController(BookingService service, CurrentMemberService member, OnlineDepositService payments) {
        this.service = service;
        this.member = member;
        this.payments = payments;
    }

    @GetMapping("/bookings")
    public String page(Principal principal, @RequestParam(defaultValue = "0") int page,
                       Model model) {
        BookingPageResponse history = service.memberHistory(member.requireMemberId(principal), page, 10);
        model.addAttribute("history", history);
        return "booking/list";
    }

    @GetMapping("/bookings/{id}")
    public String detail(Principal principal, @PathVariable Long id, Model model) {
        BookingDetailResponse detail = service.memberDetails(member.requireMemberId(principal), id);
        model.addAttribute("detail", detail);
        model.addAttribute("today", LocalDate.now());
        if ("ONLINE".equals(detail.booking().paymentMethod())) {
            model.addAttribute("depositStatus", payments.status(id));
            model.addAttribute("onlineEnabled", payments.enabled());
        }
        return "booking/detail";
    }

    @PostMapping("/bookings/{id}/early-return")
    public String earlyReturn(Principal principal, @PathVariable Long id,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              RedirectAttributes flash) {
        try {
            service.requestEarlyReturn(member.requireMemberId(principal), id, date);
            flash.addFlashAttribute("message", "Early return requested. Staff will confirm when the vehicle is returned.");
        } catch (ResponseStatusException e) {
            if (!e.getStatusCode().equals(HttpStatus.BAD_REQUEST) && !e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw e;
            }
            flash.addFlashAttribute("message", e.getReason());
        }
        return "redirect:/bookings/" + id;
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(Principal principal, @PathVariable Long id, RedirectAttributes flash) {
        Map<String, Object> result = service.memberCancel(member.requireMemberId(principal), id);
        flash.addFlashAttribute("message", "Booking cancelled. Refund request: "
                + result.get("refundRequested") + " VND (" + result.get("refundStatus") + ")");
        return "redirect:/bookings/" + id;
    }

    @GetMapping("/api/bookings")
    @ResponseBody
    public BookingPageResponse apiList(Principal principal,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return service.memberHistory(member.requireMemberId(principal), page, size);
    }

    @GetMapping("/api/bookings/{id}")
    @ResponseBody
    public BookingDetailResponse apiDetail(Principal principal, @PathVariable Long id) {
        return service.memberDetails(member.requireMemberId(principal), id);
    }

    @PostMapping("/api/bookings/{id}/cancel")
    @ResponseBody
    public Map<String, Object> apiCancel(Principal principal, @PathVariable Long id) {
        return service.memberCancel(member.requireMemberId(principal), id);
    }
}
