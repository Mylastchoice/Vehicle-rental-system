package com.morrow.vehiclerental.controller;

import com.morrow.vehiclerental.service.CatalogService;
import com.morrow.vehiclerental.repository.CategoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
public class CatalogController {
    private final CatalogService catalog;
    private final CategoryRepository categories;

    public CatalogController(CatalogService catalog, CategoryRepository categories) {
        this.catalog = catalog;
        this.categories = categories;
    }

    @GetMapping({"/", "/vehicles"})
    public String browse(@RequestParam(required = false) Long categoryId,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "newest") String sort,
                         Model model) {
        model.addAttribute("vehicles", catalog.browse(categoryId, page, sort));
        model.addAttribute("categories", categories.findAll());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        return "vehicles";
    }

    @GetMapping("/vehicles/search")
    public String search(@RequestParam(defaultValue = "") String keyword,
                         @RequestParam(required = false) String pickupDate,
                         @RequestParam(required = false) String returnDate,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        String error = validateDates(pickupDate, returnDate);
        if (error != null) {
            model.addAttribute("error", error);
        }

        model.addAttribute("vehicles", catalog.search(keyword, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("pickupDate", pickupDate);
        model.addAttribute("returnDate", returnDate);
        // Cần tích hợp AvailabilityService của M3 để lọc xe còn trống theo ngày.
        return "search";
    }

    private String validateDates(String pickupDate, String returnDate) {
        if (pickupDate == null || pickupDate.isBlank()
                || returnDate == null || returnDate.isBlank()) {
            return null;
        }
        try {
            LocalDate pickup = LocalDate.parse(pickupDate);
            LocalDate returning = LocalDate.parse(returnDate);
            if (!returning.isAfter(pickup)) {
                return "Ngày trả xe phải sau ngày nhận xe.";
            }
            if (pickup.isBefore(LocalDate.now())) {
                return "Ngày nhận xe không được ở trong quá khứ.";
            }
            return null;
        } catch (DateTimeParseException ex) {
            return "Ngày nhận hoặc ngày trả xe không hợp lệ.";
        }
    }

    @GetMapping("/vehicles/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", catalog.detail(id));
        return "vehicle-detail";
    }
}
